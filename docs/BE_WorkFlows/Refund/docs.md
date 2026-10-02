# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE REFUND (CHÍNH SÁCH HOÀN TIỀN)

> Module Refund định nghĩa công cụ tính toán chính sách hoàn tiền tự động (`RefundPolicyEngine`) và quy trình xử lý hủy đơn bồi hoàn của khách hàng.

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `RefundPolicy_Engine` | Flowchart | Cây quyết định của `RefundPolicyEngine`: phân loại theo 5 nhóm lý do và tính mức hoàn tiền theo các mốc thời gian trước khởi hành |
| 2 | `RefundPolicy_CancellationFlow` | Sequence Diagram | Quy trình hủy đơn và khởi tạo hoàn tiền: Xác thực IDOR, gọi engine tính toán, tạo bản ghi Refund bất biến và cập nhật đơn sang CANCELLED |

---

## 1. RefundPolicy_Engine.png — Cây Quyết Định Chính Sách Hoàn Tiền

**Lớp nghiệp vụ chính:** `com.danasea.backend.modules.order.domain.services.RefundPolicyEngine`

**Quy tắc phân loại theo lý do hoàn tiền (`RefundReason`):**

1. **Nhóm bất khả kháng hoặc lỗi từ nhà cung cấp (`WEATHER`, `VENDOR_FAULT`):**
   - Hoàn **100%** giá trị đơn hàng bất kể thời điểm hủy.

2. **Nhóm can thiệp quản trị hoặc bồi thường (`ADMIN_OVERRIDE`, `COMPENSATION`, `DISPUTE`):**
   - Nếu có cung cấp `customPercentage`: Áp dụng chính xác tỷ lệ được chỉ định.
   - Nếu `customPercentage` rỗng: Mặc định hoàn **100%**.

3. **Nhóm khách hàng chủ động yêu cầu hủy (`CUSTOMER_REQUEST`, `CUSTOMER_CANCEL`):**
   - Kiểm tra tính hợp lệ của thời gian: `cancelTime <= departureTime`. Nếu thời điểm hủy sau khi chuyến đi đã bắt đầu, mức hoàn là **0%**.
   - Tính toán số phút còn lại trước giờ khởi hành (`minutesUntilDeparture`):
     - **> 48 giờ (> 2880 phút):** Hoàn lại **100%**.
     - **Từ 24 đến 48 giờ (1440 - 2880 phút):** Hoàn lại **70%**.
     - **Từ 2 đến 24 giờ (120 - 1440 phút):** Hoàn lại **30%**.
     - **< 2 giờ (< 120 phút):** Hoàn lại **0%** (không hoàn).

4. **Các trường hợp khác hoặc lý do không xác định:** Mặc định hoàn **0%**.

---

## 2. RefundPolicy_CancellationFlow.png — Luồng Xem Trước Mức Hoàn Tiền & Thực Thi Hủy Đơn

**Kiến trúc phân tách Use Case:**
- **Query (Idempotent Preview):** `GetCancellationPreviewUseCase` (không làm biến đổi trạng thái hệ thống).
- **Command (Execute Mutation):** `RequestRefundUseCase` (tạo bản ghi hoàn tiền bất biến với Idempotency Key & cập nhật trạng thái đơn).

**Thành phần tham gia:** `Customer` -> `OrderCancellationController` -> `GetCancellationPreviewUseCase` / `RequestRefundUseCase` -> `RefundPolicyEngine` -> `SubOrderRepositoryPort` -> `RefundRepositoryPort` -> `OrderEventPublisherPort`

**Quy trình xử lý 2 giai đoạn:**

### Giai đoạn 1: Xem trước mức hoàn tiền (Cancel Preview)
1. Khách hàng gọi `GET /api/orders/sub-orders/{id}/cancel-preview`.
2. `GetCancellationPreviewUseCase` nạp thông tin đơn phụ, thực hiện **IDOR check** xác thực quyền sở hữu.
3. Gọi `RefundPolicyEngine.evaluate(CUSTOMER_REQUEST, startTime, now, subtotal)` để tính toán mức hoàn:
   - **> 48 giờ:** Hoàn lại 100%.
   - **24 đến 48 giờ:** Hoàn lại 70%.
   - **2 đến 24 giờ:** Hoàn lại 30%.
   - **< 2 giờ:** Hoàn lại 0%.
4. Trả về `CancellationPreviewResult` với tỷ lệ và số tiền dự kiến hoàn để khách hàng cân nhắc (không thay đổi dữ liệu DB).

### Giai đoạn 2: Thực thi hủy đơn & Khởi tạo hoàn tiền (Execute Cancellation & Refund)
1. Khách hàng gửi yêu cầu qua `POST /api/orders/sub-orders/{id}/cancel` kèm `{reason}` và `Idempotency-Key`.
2. `RequestRefundUseCase` kiểm tra IDOR và trạng thái đơn hàng (chỉ cho phép đơn `CONFIRMED`).
3. Đánh giá lại chính sách hoàn tiền qua `RefundPolicyEngine.evaluate(...)`.
4. Nếu số tiền hoàn `refundAmount > 0`:
   - Tạo bản ghi `Refund` ở trạng thái `PENDING` kèm lý do, số tiền và **Idempotency Key** bảo vệ chống tạo lặp giao dịch.
5. Cập nhật trạng thái đơn phụ: `SubOrder.status = CANCELLED` và hoàn trả số lượng slot tồn kho.
6. Phát sự kiện `SubOrderCancelledEvent` qua `OrderEventPublisherPort` để các module liên quan đồng bộ.
7. Trả về chi tiết kết quả hủy kèm mã giao dịch hoàn tiền cho khách hàng.
