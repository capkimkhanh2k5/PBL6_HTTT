# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE ORDER & PAYMENT (ĐƠN HÀNG & THANH TOÁN)

> Module Order & Payment chịu trách nhiệm tạo Master Order, tự động tách Sub-Orders 1:1 theo từng nhà cung cấp (Vendor), tính toán tỷ lệ hoa hồng nền tảng (Commission Policy), tích hợp thanh toán đa cổng (VNPay, PayPal) và xử lý Webhook IPN hoàn tất giao dịch an toàn, chống giả mạo chữ ký số và bảo vệ chống xử lý lặp (Idempotency).

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `Order_Creation_And_Splitting` | Sequence Diagram | Tạo Master Order từ Booking HOLD: kiểm tra Idempotency, xác thực IDOR, tách Sub-Orders 1:1 theo từng BookingItem, áp dụng tỷ lệ hoa hồng Vendor và chuyển trạng thái Booking sang `PENDING_PAYMENT` |
| 2 | `Payment_Intent_DualGateways` | Sequence Diagram | Khởi tạo Payment Intent đa cổng: phân luồng linh hoạt giữa VNPay (cổng nội địa, định dạng số tiền x100, chữ ký HMAC-SHA512) và PayPal (cổng quốc tế, chuẩn hóa quy đổi VND-USD, gọi REST API v2 Checkout) |
| 3 | `Payment_Webhook_And_Fulfillment` | Sequence Diagram | Xử lý Webhook IPN từ cổng thanh toán: xác thực chữ ký số bảo mật, cơ chế chống lặp giao dịch (Idempotency Guard), chuyển trạng thái đơn hàng sang `PAID`, xác nhận đơn phụ `CONFIRMED` và kích hoạt hoàn tất Booking |
| 4 | `Order_Payment_Lifecycle_StateMachine` | State Machine | Vòng đời trạng thái phân tách 2 trục độc lập giữa Vòng đời đơn hàng (`OrderStatus`), Trục dòng tiền thực thu (`PaymentStatus`) và Vòng đời đơn phụ của Vendor (`SubOrderStatus`) |

---

## 1. Order_Creation_And_Splitting.png — Tạo Master Order & Tách Sub-Orders 1:1

**Lớp xử lý chính:**
- `com.danasea.backend.modules.order.presentation.controllers.OrderController`
- `com.danasea.backend.modules.order.application.usecases.CreateOrderUseCase`
- `com.danasea.backend.modules.order.domain.ports.BookingLookupPort`
- `com.danasea.backend.modules.order.domain.ports.CommissionPolicyPort`
- `com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort`
- `com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort`
- `com.danasea.backend.modules.order.domain.ports.BookingStatusUpdatePort`

**Luồng nghiệp vụ chi tiết:**
1. Khách hàng gửi yêu cầu tạo đơn qua `POST /api/orders` với `{bookingId}` và header `Idempotency-Key`.
2. **Kiểm tra Idempotency:** Tra cứu qua `MasterOrderRepositoryPort.findByBookingId(bookingId)`. Nếu đơn hàng cho Booking này đã tồn tại, lập tức trả về kết quả `MasterOrder` hiện có (Idempotent response) để tránh tạo trùng lặp.
3. **Xác thực Booking & Chống IDOR:**
   - Lấy thông tin đặt chỗ qua `BookingLookupPort.findBookingForOrder(bookingId)`.
   - Xác minh người gửi yêu cầu là chủ sở hữu (`booking.customerId == currentUserId`).
   - Kiểm tra trạng thái Booking bắt buộc phải là `HOLD`. Nếu không hợp lệ, ném ngoại lệ `409 Conflict (BookingNotEligibleForOrderException)`.
4. **Tách Sub-Orders 1:1 & Tính hoa hồng (Commission Policy):**
   - Với mỗi mục đặt chỗ (`BookingItem`), hệ thống tạo một `SubOrder` riêng biệt thuộc về `vendorId` tương ứng.
   - Gọi `CommissionPolicyPort.getCommissionRate(vendorId)` để lấy tỷ lệ hoa hồng (mặc định 10% = `0.10`).
   - Tính toán tài chính minh bạch cho từng đơn phụ:
     - `subtotal = price * quantity`
     - `commissionAmount = subtotal * commissionRate`
     - `payoutAmount = subtotal - commissionAmount`
5. Lưu trữ `MasterOrder` (trạng thái `PENDING_PAYMENT`, `paymentStatus = UNPAID`) và lưu toàn bộ danh sách `SubOrder` (trạng thái `PENDING`).
6. Cập nhật trạng thái Booking sang `PENDING_PAYMENT` và phát sự kiện `OrderCreatedEvent`.
7. Phản hồi thông tin đơn hàng `201 Created` cho khách hàng.

---

## 2. Payment_Intent_DualGateways.png — Tạo Payment Intent Đa Cổng (VNPay / PayPal)

**Lớp xử lý chính:**
- `com.danasea.backend.modules.order.presentation.controllers.PaymentController`
- `com.danasea.backend.modules.order.application.usecases.CreatePaymentIntentUseCase`
- `com.danasea.backend.modules.order.infrastructure.adapters.PayPalPaymentAdapter` (@Primary)
- `com.danasea.backend.modules.order.infrastructure.adapters.VNPayPaymentAdapter`

**Luồng nghiệp vụ chi tiết:**
1. Khách hàng gửi yêu cầu thanh toán qua `POST /api/payments/{orderId}/create-intent` kèm nhà cung cấp `{provider: "VNPAY" | "PAYPAL"}`.
2. **Kiểm tra quyền sở hữu & trạng thái đơn:**
   - Kiểm tra IDOR: `order.customerId == currentUserId`.
   - Đơn hàng bắt buộc phải ở trạng thái `PENDING_PAYMENT`.
3. **Phân nhánh xử lý theo cổng thanh toán:**
   - **Cổng nội địa VNPay (`provider == "VNPAY"`):**
     - Nhân số tiền x 100 theo quy định cổng VNPay.
     - Sắp xếp tất cả các trường tham số query (`vnp_*`) theo thứ tự từ điển ASCII.
     - Sinh chữ ký mã hóa bảo mật **HMAC-SHA512** sử dụng bí mật `vnp_HashSecret`.
     - Tạo URL điều hướng sang cổng VNPay Sandbox (`https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...`).
   - **Cổng quốc tế PayPal (`provider == "PAYPAL"`):**
     - Gọi endpoint OAuth2 `/v1/oauth2/token` để lấy Bearer Access Token.
     - Quy đổi số tiền từ VNĐ sang USD theo tỷ giá cấu hình (mặc định `25,400`, đảm bảo mức tối thiểu `$1.00 USD`).
     - Gọi PayPal REST API v2 `POST /v2/checkout/orders` với chế độ `intent: "CAPTURE"`.
     - Trích xuất liên kết phê duyệt thanh toán (`rel: "approve"`).
4. Lưu bản ghi thanh toán ở trạng thái `PENDING` và trả về URL thanh toán cho khách hàng để thực hiện chuyển tiền.

---

## 3. Payment_Webhook_And_Fulfillment.png — Xử Lý Webhook IPN & Xác Nhận Đơn Hàng

**Lớp xử lý chính:**
- `com.danasea.backend.modules.order.presentation.controllers.PaymentController`
- `com.danasea.backend.modules.order.application.usecases.HandleWebhookUseCase`
- `com.danasea.backend.modules.order.domain.ports.PaymentGatewayPort`
- `com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort`
- `com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort`
- `com.danasea.backend.modules.order.domain.ports.BookingStatusUpdatePort`

**Luồng nghiệp vụ chi tiết:**
1. Cổng thanh toán (VNPay hoặc PayPal) gửi HTTP POST Webhook IPN đến `/api/payments/webhook/{provider}` kèm payload giao dịch và chữ ký số.
2. **Xác thực chữ ký số bảo mật:**
   - Với VNPay: Xác thực chữ ký `vnp_SecureHash` (HMAC-SHA512).
   - Với PayPal: Xác thực webhook signature qua chứng chỉ PayPal Certificate.
   - Nếu chữ ký bị làm giả hoặc sai lệch, ném `PaymentVerificationException` và từ chối với `400 Bad Request`.
3. **Bảo vệ Idempotency chống xử lý lặp:**
   - Nếu `MasterOrder` đã ở trạng thái `PAID` trước đó, hệ thống nhận diện giao dịch bị lặp và trả về ngay `200 OK` (No-op).
4. **Cập nhật trạng thái Đơn hàng & Đơn phụ:**
   - Thực thi chuyển đổi trạng thái nguyên tử: `order.markPaid()` -> `MasterOrder.status = PAID`, `paymentStatus = PAID`.
   - Cập nhật toàn bộ các đơn phụ `SubOrder` sang trạng thái `CONFIRMED` (sẵn sàng cho khách hàng tạo mã QR Check-in trải nghiệm dịch vụ).
5. **Kích hoạt hoàn tất Booking:**
   - Gọi `BookingStatusUpdatePort.confirmBooking(bookingId, customerId)` để chuyển trạng thái Booking sang `CONFIRMED` trong cùng transaction.
6. Phát sự kiện `PaymentSuccessEvent` sang Event Bus để kích hoạt các module phụ trợ (gửi thông báo, email xác nhận).
7. Phản hồi `200 OK` cho cổng thanh toán để hoàn tất handshake IPN.

---

## 4. Order_Payment_Lifecycle_StateMachine.png — Vòng Đời Trạng Thái Đơn Hàng & Dòng Tiền

Sơ đồ biểu diễn mô hình máy trạng thái phân tách 2 trục độc lập giữa **Vòng đời thực hiện (Fulfillment)** và **Dòng tiền (Cashflow)**:

- **Trục Master Order Fulfillment (`OrderStatus`):**
  - `PENDING_PAYMENT`: Khởi tạo từ Booking HOLD (UNPAID).
  - `PAID`: Khớp Webhook thanh toán thành công (Thanh toán 100%).
  - `CANCELLED`: Quá hạn 15 phút không thanh toán hoặc khách chủ động hủy đơn.
  - `COMPLETED`: Toàn bộ Sub-Orders đã hoàn tất check-in và phục vụ dịch vụ.
  - `PARTIALLY_COMPLETED`: Một số Sub-Orders hoàn tất, một số bị hủy hoặc hoàn tiền.

- **Trục Dòng Tiền Thực Thu (`PaymentStatus`):**
  - `UNPAID`: Chưa phát sinh giao dịch tiền tệ.
  - `CASH_PAID`: Đã ghi nhận dòng tiền 100% từ cổng thanh toán.
  - `REFUNDED`: Đã hoàn lại toàn phần hoặc một phần tiền cho khách hàng (theo chính sách hủy hoặc khiếu nại).
  - `NO_REFUND`: Khách hàng hủy trễ (<= 24 giờ trước khởi hành) vi phạm chính sách mốc thời gian, giữ lại 100% tiền vé.

- **Vòng Đời Đơn Phụ Của Vendor (`SubOrderStatus`):**
  - `PENDING` -> `CONFIRMED` (Khi Master Order chuyển sang `PAID`) -> `CHECKED_IN` (Quét QR tại thực địa) -> `COMPLETED` (Kết thúc phục vụ).
  - Phân nhánh hủy: `CONFIRMED` -> `CANCELLED` hoặc `REFUNDED` nếu phát sinh hủy chuyến hoặc bồi hoàn.
