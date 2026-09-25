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

## 2. RefundPolicy_CancellationFlow.png — Luồng Hủy Đơn & Tạo Hoàn Tiền

**Thành phần tham gia:** `Customer` -> `OrderPaymentService` -> `RefundPolicyEngine` -> `JpaSubOrderRepository` -> `JpaRefundRepository`

**Quy trình xử lý:**
1. Khách hàng gửi yêu cầu hủy qua `POST /api/orders/{id}/cancel` kèm `{reason, subOrderId}`.
2. `OrderPaymentService` nạp thông tin đơn phụ, thực hiện **IDOR check** để xác nhận quyền sở hữu của khách hàng.
3. Gọi `RefundPolicyEngine.evaluate(...)` với thông tin khung giờ dịch vụ và thời điểm hủy để tính toán số tiền hoàn hợp lệ.
4. Nếu số tiền hoàn `refundAmount > 0`:
   - Tạo bản ghi `Refund` ở trạng thái `PENDING` kèm lý do và số tiền đã được tính.
   - Đính kèm **Idempotency Key** để tránh lặp giao dịch hoàn tiền trên cổng thanh toán.
5. Cập nhật trạng thái đơn phụ: `SubOrder.status = CANCELLED`.
6. Trả về chi tiết kết quả hủy kèm tỷ lệ và số tiền hoàn cho khách hàng.
