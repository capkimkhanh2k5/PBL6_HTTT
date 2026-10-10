# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE REFUND (CHÍNH SÁCH HOÀN TIỀN)

> Module Refund định nghĩa công cụ tính toán chính sách hoàn tiền tự động (`RefundPolicyEngine`) và quy trình xử lý hủy đơn bồi hoàn của khách hàng.

| # | Sơ đồ | Loại | Mã nguồn Mermaid (.mmd) | Nội dung |
|---|-------|------|--------------------------|----------|
| 1 | `RefundPolicy_Engine_DF` | Decision Flowchart | [`codeFlows/Decision_Flowchart/RefundPolicy_Engine_DF.mmd`](codeFlows/Decision_Flowchart/RefundPolicy_Engine_DF.mmd) | Cây quyết định của `RefundPolicyEngine`: phân loại theo 5 nhóm lý do và tính mức hoàn tiền theo các mốc thời gian trước khởi hành |
| 2 | `RefundPolicy_Cancellation_SD` | Sequence Diagram | [`codeFlows/Sequence_Diagram/RefundPolicy_Cancellation_SD.mmd`](codeFlows/Sequence_Diagram/RefundPolicy_Cancellation_SD.mmd) | Quy trình hủy đơn và khởi tạo hoàn tiền 2 giai đoạn: Preview (Idempotent Query) & Execute (Command DB Transaction) |
| 3 | `Refund_Processing_Pipeline_SD` | Sequence Diagram | [`codeFlows/Sequence_Diagram/Refund_Processing_Pipeline_SD.mmd`](codeFlows/Sequence_Diagram/Refund_Processing_Pipeline_SD.mmd) | Luồng thực thi hoàn tiền thực tế qua cổng VNPay / PayPal và cơ chế retry Exponential Backoff của Background Job |

---

## 1. RefundPolicy_Engine_DF.png — Cây Quyết Định Chính Sách Hoàn Tiền

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

## 2. RefundPolicy_Cancellation_SD.png — Luồng Xem Trước Mức Hoàn Tiền & Thực Thi Hủy Đơn

**Kiến trúc phân tách Use Case:**
- **Query (Idempotent Preview):** `GetCancellationPreviewUseCase` (không làm biến đổi trạng thái hệ thống).
- **Command (Execute Mutation):** `RequestRefundUseCase` (tạo bản ghi hoàn tiền bất biến với Idempotency Key & cập nhật trạng thái đơn).

**Thành phần tham gia:** `Customer` -> `OrderCancellationController` -> `GetCancellationPreviewUseCase` / `RequestRefundUseCase` -> `RefundPolicyEngine` -> `SubOrderRepositoryPort` -> `RefundRepositoryPort` -> `OrderEventPublisherPort`

**Quy trình xử lý 2 giai đoạn:**

### Giai đoạn 1: Xem trước mức hoàn tiền (Cancel Preview)
1. Khách hàng gọi `GET /api/orders/sub-orders/{id}/cancel-preview`.
2. `GetCancellationPreviewUseCase` nạp thông tin đơn phụ, thực hiện **IDOR check** xác thực quyền sở hữu.
3. Gọi `RefundPolicyEngine.evaluate(CUSTOMER_REQUEST, startTime, now, originalAmount)` để tính toán mức hoàn:
   - `originalAmount` được lấy chính xác từ `subOrder.finalAmount` (số tiền thực trả sau khi đã áp dụng voucher giảm giá). Tuyệt đối không hoàn phần voucher trợ giá bằng tiền mặt (Xem chi tiết tại [Module Discount](../Discount/docs.md)).
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
   - Bản ghi này lưu trữ đầy đủ `subOrderId`, `paymentId`, số tiền cần hoàn và số lần thử lại (`retryCount = 0`).
5. Cập nhật trạng thái đơn phụ: `SubOrder.status = CANCELLED` và hoàn trả số lượng slot tồn kho (`serviceSlotPort.releaseBookedSlot`).
6. Phát sự kiện `SubOrderCancelledEvent` qua `OrderEventPublisherPort` để các module liên quan đồng bộ.
7. Trả về chi tiết kết quả hủy kèm mã giao dịch hoàn tiền cho khách hàng.

---

### Giai đoạn 3: Thực thi chuyển tiền hoàn thực tế qua Cổng thanh toán (Refund Processing Pipeline)

**Thành phần xử lý:** `RefundProcessingService` & `RefundProcessingJob` → `PaymentGatewayPort` (`PayPalPaymentAdapter` / `VNPayPaymentAdapter`) → `PostgreSQL`

Các bản ghi hoàn tiền `Refund (status = PENDING)` được khởi tạo từ nhiều nguồn trong hệ thống:
- Khách hàng chủ động hủy đơn (`RequestRefundUseCase`).
- Hủy đặt chỗ / Vendor từ chối phục vụ (`BookingCancellationFinancialAdapter`).
- Admin phê duyệt khiếu nại (`ResolveDisputeUseCase`).
- Admin duyệt hủy tour do thời tiết xấu cảnh báo đỏ (`AdminWeatherAlertController`).

**Quy trình xử lý hoàn tiền thực tế:**
1. `RefundProcessingService.processRefund(refundId)` truy vấn thông tin thanh toán gốc (`Payment`) thành công của đơn hàng để lấy:
   - Cổng thanh toán gốc (`VNPAY` hoặc `PAYPAL`).
   - Mã tham chiếu giao dịch gốc (`providerPaymentId`, hoặc `providerCaptureId` đối với PayPal).
2. Xây dựng yêu cầu `GatewayRefundRequest` và gọi `PaymentGatewayPort.refund(...)`:
   - **Với PayPal:** Gọi PayPal REST API v2 `POST /v2/payments/captures/{capture_id}/refund` với số tiền quy đổi USD tương ứng, lưu lại `providerRefundId` trả về từ PayPal.
   - **Với VNPay:** Gọi API hoàn tiền VNPay (`vnp_Command=refund`) với chữ ký HMAC-SHA512 và mã giao dịch gốc.
3. **Cập nhật trạng thái & Xử lý sự cố:**
   - **Thành công (`SUCCESS`):** Cập nhật `Refund.status = PROCESSED`, lưu `providerRefundId`, ghi nhận thời gian `processedAt`. Đồng bộ cập nhật trạng thái dòng tiền `MasterOrder.paymentStatus = REFUNDED`.
   - **Bị từ chối (`REJECTED`):** Cập nhật `Refund.status = FAILED`, ghi nhận `failureReason` từ cổng.
   - **Lỗi mạng / Timeout cổng (`RETRYABLE`):** Tăng `retryCount`, thiết lập thời gian thử lại tiếp theo (`nextRetryAt`) theo chiến lược giãn cách thời gian (Exponential Backoff), giữ nguyên trạng thái `PENDING`.
   - Nếu số lần thử lại vượt quá hạn mức (ví dụ 5 lần): Chuyển sang `FAILED` và ghi log cảnh báo để quản trị viên can thiệp thủ công.
4. **Scheduled Background Job (`RefundProcessingJob`):**
   - Chạy định kỳ (mỗi 1 phút) tự động quét các bản ghi `Refund` ở trạng thái `PENDING` có `nextRetryAt <= now()` để tự động kích hoạt `RefundProcessingService.processPendingRefunds()`.

---

### Giai đoạn 4: Quản lý & Giám sát Hoàn tiền (Admin & Customer APIs)

> 📌 *Chi tiết sơ đồ Mermaid và luồng nghiệp vụ:* xem tại [`../Order_Payment/Admin_Transaction_Refund_Management_Workflows.md`](../Order_Payment/Admin_Transaction_Refund_Management_Workflows.md).

1. **Dành cho Quản trị viên (Admin Portal):**
   - `GET /api/admin/refunds`: Tra cứu danh sách các khoản hoàn tiền toàn sàn (phân trang `Pageable`, lọc nâng cao theo `status`, `reason`, `subOrderId`, `orderId`, `provider`, `vendorId`, `customerId`, khoảng thời gian `from/to`).
   - `GET /api/admin/refunds/{id}`: Xem chi tiết khoản hoàn tiền, mã tham chiếu cổng, số lần thử lại (`retryCount`), thời gian xử lý và nguyên nhân lỗi (nếu có).
2. **Dành cho Khách hàng & Quản trị viên:**
   - `GET /api/orders/{orderId}/refunds`: Tra cứu tiến trình và trạng thái các khoản hoàn tiền thuộc đơn hàng.
   - **Bảo mật IDOR Guard:** Kiểm tra nghiêm ngặt quyền sở hữu `order.customerId == currentUserId` hoặc quyền `ROLE_ADMIN` để ngăn chặn truy cập trái phép từ tài khoản khác (HTTP 403 Forbidden).
