# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE ORDER & PAYMENT (ĐƠN HÀNG & THANH TOÁN)

> Module Order & Payment chịu trách nhiệm tạo Master Order, tự động tách Sub-Orders 1:1 theo từng nhà cung cấp (Vendor), tính toán tỷ lệ hoa hồng nền tảng (Commission Policy), tích hợp thanh toán đa cổng (VNPay, PayPal) và xử lý Webhook IPN hoàn tất giao dịch an toàn, chống giả mạo chữ ký số và bảo vệ chống xử lý lặp (Idempotency).

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `Order_Creation_And_Splitting` | Sequence Diagram | Tạo Master Order từ Booking HOLD: kiểm tra Idempotency, xác thực IDOR, tách Sub-Orders 1:1 theo từng BookingItem, áp dụng tỷ lệ hoa hồng Vendor và chuyển trạng thái Booking sang `PENDING_PAYMENT` |
| 2 | `Payment_Intent_DualGateways` | Sequence Diagram | Khởi tạo Payment Intent đa cổng: phân luồng linh hoạt giữa VNPay (cổng nội địa, định dạng số tiền x100, chữ ký HMAC-SHA512) và PayPal (cổng quốc tế, chuẩn hóa quy đổi VND-USD, gọi REST API v2 Checkout) |
| 3 | `Payment_Webhook_And_Fulfillment` | Sequence Diagram | Xử lý Webhook IPN từ cổng thanh toán: xác thực chữ ký số bảo mật, cơ chế chống lặp giao dịch (Idempotency Guard), chuyển trạng thái đơn hàng sang `PAID`, xác nhận đơn phụ `CONFIRMED` và kích hoạt hoàn tất Booking |
| 4 | `Order_Payment_Lifecycle_StateMachine` | State Machine | Vòng đời trạng thái phân tách 2 trục độc lập giữa Vòng đời đơn hàng (`OrderStatus`), Trục dòng tiền thực thu (`PaymentStatus`) và Vòng đời đơn phụ của Vendor (`SubOrderStatus`) |
| 5 | `EPIC04_Architecture_And_UseCases` | Flowchart | Kiến trúc phân lớp Clean Architecture tách biệt các Use Cases: Tạo đơn, Đa cổng thanh toán, Webhook, Xem trước mức hoàn tiền và Khởi tạo hoàn tiền |
| 6 | `Admin_MultiFilter_Listing_Flow` | Sequence Diagram | Quản trị toàn sàn: Lọc đa chiều đơn hàng, giao dịch thanh toán và hoàn tiền cho Admin (`GET /api/admin/orders`, `/payments`, `/refunds`), tối ưu hóa subquery `EXISTS` và gom nhóm tránh N+1 |
| 7 | `Automated_Payment_Reconciliation_Flow` | Sequence Diagram | Đối soát tự động định kỳ (`PaymentReconciliationJob`): Quét pending payments > 2 phút, gọi VNPay QueryDR (HMAC-SHA512) & PayPal Capture, cập nhật trạng thái đơn hàng và ghi nhận nhật ký kiểm toán `AuditLogInternalApi` |

---

### 1. Order_Creation_And_Splitting.png — Tạo Master Order & Tách Sub-Orders 1:1

**Luồng nghiệp vụ chi tiết:**
1. Khách hàng gửi yêu cầu tạo đơn qua `POST /api/orders` với `{bookingId}` và header `Idempotency-Key`.
2. **Kiểm tra Idempotency & Bảo vệ quyền sở hữu (IDOR Guard):**
   - **Nhánh tra cứu theo `bookingId` (`findByBookingId`):** Nếu đơn hàng cho Booking này đã tồn tại, hệ thống **bắt buộc kiểm tra quyền sở hữu**: `order.getCustomerId().equals(currentUserId)`.
     - Nếu khớp: Lập tức trả về kết quả `MasterOrder` hiện có (`201 Created` / Idempotent response).
     - Nếu không khớp (User B cố tình gửi lại `bookingId` của User A): Ném ngay `UnauthorizedOrderAccessException` trả về **HTTP 403 Forbidden** (`UNAUTHORIZED_ORDER_ACCESS`), triệt tiêu nguy cơ rò rỉ dữ liệu đơn hàng và thông tin cá nhân.
   - **Nhánh tra cứu theo Idempotency Key (`findByCustomerIdAndIdempotencyKey`):** Kiểm tra quyền sở hữu tương tự và xác thực `bookingId` khớp với đơn cũ (`409 Conflict` nếu cùng key nhưng khác booking).
3. **Xác thực Booking & Chống IDOR (Nhánh tạo mới):**
   - Lấy thông tin đặt chỗ qua `BookingLookupPort.findBookingForOrder(bookingId)`.
   - Xác minh người gửi yêu cầu là chủ sở hữu (`booking.customerId == currentUserId`). Nếu vi phạm → **HTTP 403 Forbidden**.
   - Kiểm tra trạng thái Booking bắt buộc phải là `HOLD`. Nếu không hợp lệ, ném ngoại lệ `409 Conflict (BookingNotEligibleForOrderException)`.
4. **Tách Sub-Orders 1:1 & Tính hoa hồng (Commission Policy):**
   - Với mỗi mục đặt chỗ (`BookingItem`), hệ thống tạo một `SubOrder` riêng biệt thuộc về `vendorId` tương ứng.
   - Gọi `CommissionPolicyPort.getCommissionRate(vendorId)` để lấy tỷ lệ hoa hồng (mặc định 10% = `0.10`).
   - Tính toán tài chính minh bạch cho từng đơn phụ:
     - `subtotal = price * quantity`
     - `commissionAmount = subtotal * commissionRate`
     - `payoutAmount = subtotal - commissionAmount`
5. Lưu trữ `MasterOrder` (trạng thái `PENDING_PAYMENT`, `paymentStatus = UNPAID`) và lưu toàn bộ danh sách `SubOrder` (trạng thái `PENDING`).
6. **Áp dụng Voucher & Phân bổ chiết khấu (Discount Enhancement):** Nếu đơn hàng có gửi kèm `discountCode`, hệ thống thực thi khóa bi quan (`findByCodeForUpdate`), kiểm tra quota toàn sàn và từng user, phân bổ chiết khấu theo giải thuật Hare-Niemeyer vào từng SubOrder (`finalAmount`, `commissionBasisAmount`), tăng `usedCount` nguyên tử và lưu `DiscountRedemption` (Xem chi tiết tại [Module Discount](../Discount/docs.md)).
7. Cập nhật trạng thái Booking sang `PENDING_PAYMENT` và phát sự kiện `OrderCreatedEvent`.
8. Phản hồi thông tin đơn hàng `201 Created` cho khách hàng.

---

## 2. Payment_Intent_DualGateways.png — Tạo Payment Intent Đa Cổng, Khóa Tuần Tự & Bền Vững Hóa

**Luồng nghiệp vụ chi tiết:**
1. Khách hàng gửi yêu cầu thanh toán qua `POST /api/payments/{orderId}/create-intent` kèm `{provider: "VNPAY" | "PAYPAL"}`.
2. **Kiểm tra quyền sở hữu & trạng thái đơn:**
   - Kiểm tra IDOR: `order.customerId == currentUserId` (403 Forbidden nếu không phải chủ đơn).
   - Đơn hàng bắt buộc phải ở trạng thái `PENDING_PAYMENT` (409 Conflict nếu đơn đã thanh toán hoặc đã hủy).
3. **Cơ chế khóa tuần tự & Bền vững hóa (Pessimistic Lock & Idempotent Persistence):**
   - Áp dụng khóa tuần tự: Khóa bản ghi `Payment` theo `orderId`, sau đó khóa `MasterOrder` trong DB để loại trừ xung đột đồng thời (Race Condition) khi người dùng bấm gửi nhiều yêu cầu liên tiếp.
   - **Tạo và Commit Payment trước khi gọi cổng:** Bản ghi `Payment` ở trạng thái `PENDING` được lưu và commit vào PostgreSQL trước khi gửi yêu cầu sang gateway. Khi xảy ra timeout hoặc người dùng retry, hệ thống tái sử dụng đúng UUID của Payment `PENDING` đó, ngăn ngừa tạo ra các bản ghi rác.
4. **Phân nhánh xử lý theo cổng thanh toán:**
   - **Cổng nội địa VNPay (`provider == "VNPAY"`):**
     - Nhân số tiền x 100 theo quy định cổng VNPay.
     - Sắp xếp tất cả các trường tham số query (`vnp_*`) theo thứ tự từ điển ASCII.
     - Sinh chữ ký mã hóa bảo mật **HMAC-SHA512** sử dụng bí mật `vnp_HashSecret`.
     - Tạo URL điều hướng sang cổng VNPay Sandbox (`https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...`).
   - **Cổng quốc tế PayPal (`provider == "PAYPAL"`):**
     - Gọi endpoint OAuth2 `/v1/oauth2/token` để lấy Bearer Access Token.
     - Quy đổi số tiền từ VNĐ sang USD theo tỷ giá cấu hình (mặc định `25,400`, đảm bảo mức tối thiểu `$1.00 USD`).
     - Gọi PayPal REST API v2 `POST /v2/checkout/orders` với chế độ `intent: "CAPTURE"`, truyền `PayPal-Request-Id` bằng Payment UUID để đảm bảo tính Idempotent trên cổng PayPal.
     - Lưu `providerOrderId` (PayPal Order ID) vào bản ghi `Payment` để phục vụ bước Capture tiếp theo.
     - Trích xuất liên kết phê duyệt thanh toán (`rel: "approve"`).
5. **Hoàn tất bước Capture PayPal (`POST /api/payments/paypal/capture`):**
   - Sau khi khách hàng duyệt thanh toán trên giao diện PayPal, client gửi yêu cầu capture kèm `orderId` và `paypalOrderId`.
   - `OrderPaymentService` gọi PayPal REST API v2 `POST /v2/checkout/orders/{id}/capture` để khớp dòng tiền, lưu `providerCaptureId` và cập nhật đơn hàng thành `PAID`.
6. **Đối soát tự động (Payment Reconciliation Job):**
   - `PaymentReconciliationJob` chạy ngầm định kỳ quét các `Payment` ở trạng thái `PENDING` quá thời gian quy định (ví dụ quá 15 phút) để tự động truy vấn trạng thái từ cổng thanh toán và đồng bộ dữ liệu.
7. **Quản trị thanh toán (Admin Payment Management):**
   - `GET /api/admin/payments`: Xem danh sách toàn bộ các thanh toán trong hệ thống (phân trang, lọc theo trạng thái).
   - `GET /api/admin/payments/{id}`: Tra cứu chi tiết thanh toán, mã tham chiếu cổng, lịch sử capture và snapshot giao dịch.

---

## 3. Payment_Webhook_And_Fulfillment.png — Xử Lý Webhook IPN & Xác Nhận Đơn Hàng

**Luồng nghiệp vụ chi tiết:**
1. Cổng thanh toán (VNPay hoặc PayPal) gửi HTTP POST Webhook IPN đến `/api/payments/webhook/{provider}` kèm payload giao dịch và chữ ký số.
2. **Xác thực chữ ký số bảo mật thực tế:**
   - **Với VNPay:** Xác thực chữ ký `vnp_SecureHash` (HMAC-SHA512) dựa trên toàn bộ các tham số phản hồi.
   - **Với PayPal:** Xác thực webhook signature thực tế qua PayPal REST API v2 (`POST /v1/notifications/verify-webhook-signature`) bằng trọn bộ transmission headers (`PAYPAL-AUTH-ALGO`, `PAYPAL-CERT-URL`, `PAYPAL-TRANSMISSION-ID`, `PAYPAL-TRANSMISSION-SIG`, `PAYPAL-TRANSMISSION-TIME`) và raw JSON body.
   - **Môi trường Test / Dev nội bộ:** Bổ sung endpoint riêng `/api/internal/payments/webhook/{provider}` sử dụng chữ ký HMAC-SHA256 DANASEA, chỉ được kích hoạt trong profile `dev` hoặc `test` để kiểm thử giả lập an toàn.
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
  - `PAID`: Khớp Webhook thanh toán thành công hoặc Capture thành công (Thanh toán 100%).
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

---

## 5. EPIC04_Architecture_And_UseCases.png — Kiến Trúc Clean Architecture Phân Tách Use Cases

Sơ đồ tổng quan toàn bộ kiến trúc phân tầng chuẩn Clean Architecture của EPIC-04:

1. **Client Tier:** React Web, Mobile App và Portal Quản trị (Admin Portal).
2. **REST Controllers (Interface Adapters):**
   - `OrderController`: Tiếp nhận tạo đơn hàng từ Booking, tra cứu danh sách đơn và lịch sử hoàn tiền đơn hàng.
   - `PaymentController`: Khởi tạo Intent, thực hiện Capture PayPal và đón nhận Webhook IPN từ cổng thanh toán.
   - `AdminPaymentController`: Quản lý danh sách và chi tiết các giao dịch thanh toán phía quản trị.
   - `AdminRefundController`: Quản lý danh sách và chi tiết các yêu cầu hoàn tiền phía quản trị.
   - `InternalPaymentWebhookController`: Đón nhận webhook mô phỏng nội bộ (chỉ bật ở profile dev/test).
   - `OrderCancellationController`: Tiếp nhận yêu cầu xem trước và thực thi hủy đơn từ khách hàng.
3. **Application Use Cases & Services:**
   - `CreateOrderUseCase` (Command): Tách Sub-Orders 1:1, kiểm tra quyền sở hữu IDOR trên mọi nhánh Idempotency và tính hoa hồng nền tảng.
   - `CreatePaymentIntentUseCase` (Command): Khóa tuần tự hóa Payment/Order, lưu trước Payment PENDING và phân luồng tạo link thanh toán VNPay / PayPal.
   - `OrderPaymentService`: Quản lý nghiệp vụ thanh toán, điều phối Capture PayPal và liên kết cổng.
   - `RefundProcessingService`: Quản lý vòng đời hoàn tiền, gửi lệnh hoàn tiền thực tế sang cổng thanh toán.
   - `HandleWebhookUseCase` (Command): Khớp Webhook, xác thực chữ ký thực tế và kích hoạt hoàn tất đơn.
   - `GetCancellationPreviewUseCase` (Query): Tra cứu % và số tiền hoàn tiền, đảm bảo tính Idempotent và không biến đổi dữ liệu.
   - `RequestRefundUseCase` (Command): Tạo bản ghi Refund PENDING với Idempotency Key, cập nhật trạng thái đơn sang CANCELLED và nhả slot tồn kho.
   - `PaymentReconciliationJob` (Scheduled Job): Định kỳ đối soát trạng thái các giao dịch PENDING treo.
   - `RefundProcessingJob` (Scheduled Job): Định kỳ quét và xử lý các bản ghi Refund PENDING gửi sang cổng thanh toán.
4. **Domain Core & Policy Engines:**
   - `RefundPolicyEngine`: Tính toán tỷ lệ hoàn tiền tự động theo nhóm lý do và mốc giờ.
   - `CommissionPolicyEngine`: Tính toán hoa hồng và đối soát cho từng Vendor.
5. **Infrastructure Ports & Adapters:**
   - `PaymentGatewayPort` -> `VNPayPaymentAdapter` & `PayPalPaymentAdapter`.
   - `MasterOrderRepositoryPort`, `SubOrderRepositoryPort`, `PaymentRepositoryPort`, `RefundRepositoryPort`, `OrderEventPublisherPort`.

---

## 6. Admin_MultiFilter_Listing_Flow — Quản Trị Đơn Hàng, Giao Dịch & Hoàn Tiền Toàn Sàn

> 📌 *Chi tiết sơ đồ Mermaid và đặc tả API:* xem tại [`Admin_Transaction_Refund_Management_Workflows.md`](./Admin_Transaction_Refund_Management_Workflows.md).

**Lớp xử lý chính:**
- `com.danasea.backend.modules.order.presentation.controllers.AdminOrderController`
- `com.danasea.backend.modules.order.presentation.controllers.AdminPaymentController`
- `com.danasea.backend.modules.order.presentation.controllers.AdminRefundController`
- `com.danasea.backend.modules.order.application.OrderPaymentService`

**Luồng nghiệp vụ chi tiết:**
1. **Quản trị Đơn hàng (`GET /api/admin/orders`):**
   - Hỗ trợ lọc đa chiều: `status`, `paymentStatus`, `customerId`, `vendorId`, `fromDate`/`from`, `toDate`/`to`, phân trang `Pageable`.
   - Lọc theo nhà cung cấp (`vendorId`) thông qua truy vấn `EXISTS (SELECT 1 FROM sub_orders so WHERE so.master_order_id = m.id AND so.vendor_id = :vendorId)`.
   - Áp dụng kỹ thuật Batch Query `subOrderRepository.findByMasterOrderIdIn(orderIds)` để gom nhóm danh sách `vendorIds` và tính `totalItems` trên RAM, loại trừ triệt để lỗi N+1 Query.
   - Trả về DTO tóm tắt tối ưu `AdminOrderSummaryResponse`.
2. **Quản trị Giao dịch Thanh toán (`GET /api/admin/payments` & `GET /api/admin/payments/{id}`):**
   - Lọc theo `status`, `provider`, `orderId`, `vendorId`, `customerId`, thời gian.
   - Trả về `PaymentResponse` với đầy đủ thông tin cổng, mã giao dịch, thời điểm hết hạn và URL thanh toán.
3. **Quản trị Yêu cầu Hoàn tiền (`GET /api/admin/refunds` & `GET /api/admin/refunds/{id}`):**
   - Lọc theo `status`, `reason`, `subOrderId`, `orderId`, `provider`, `vendorId`, `customerId`, thời gian.
   - Trả về `RefundDetailResponse` kèm thông tin retry, mã lỗi cổng và tiến trình đối soát.
4. **Lịch sử Giao dịch theo Đơn hàng (`GET /api/orders/{id}/payments` & `GET /api/orders/{id}/refunds`):**
   - Tiếp nhận yêu cầu từ Khách hàng hoặc Quản trị viên.
   - **IDOR Guard:** Kiểm tra quyền sở hữu `order.customerId == currentUserId` hoặc quyền `ROLE_ADMIN`. Chặn đứng ngay lập tức với `403 Forbidden` (`AccessDeniedException`) nếu tài khoản khác cố tình truy cập.

---

## 7. Automated_Payment_Reconciliation_Flow — Cơ Chế Đối Soát Tự Động & VNPay QueryDR / PayPal Capture

> 📌 *Chi tiết sơ đồ Mermaid và đặc tả API:* xem tại [`Admin_Transaction_Refund_Management_Workflows.md`](./Admin_Transaction_Refund_Management_Workflows.md).

**Lớp xử lý chính:**
- `com.danasea.backend.modules.order.infrastructure.jobs.PaymentReconciliationJob`
- `com.danasea.backend.modules.order.domain.ports.PaymentGatewayPort`
- `com.danasea.backend.modules.order.infrastructure.adapters.VNPayPaymentAdapter`
- `com.danasea.backend.modules.order.infrastructure.adapters.PayPalPaymentAdapter`
- `com.danasea.backend.modules.audit.application.api.AuditLogInternalApi`

**Luồng nghiệp vụ chi tiết:**
1. **Quét nền định kỳ:** `PaymentReconciliationJob` kích hoạt mỗi 60 giây (quét các giao dịch `PENDING` có `createdAt < now - 2 phút`).
2. **Phân luồng đối soát theo cổng:**
   - **VNPay QueryDR (`vnp_Command=querydr`):** Tạo checksum HMAC-SHA512 với `vnp_HashSecret`, gửi request sang VNPay Merchant API. Nếu nhận `vnp_ResponseCode="00"` và `vnp_TransactionStatus="00"`, đánh dấu giao dịch `COMPLETED`.
   - **PayPal Capture / Order Query:** Tra cứu thông tin capture/order trực tiếp qua PayPal REST API v2.
3. **Đồng bộ trạng thái liên hoàn:**
   - Khi cổng xác nhận thành công: `Payment` -> `SUCCESS`, `MasterOrder` -> `PAID`, toàn bộ `SubOrder` -> `CONFIRMED`, và tự động kích hoạt `ConfirmBookingUseCase.execute(...)`.
   - Khi cổng xác nhận thất bại: `Payment` -> `FAILED` kèm lý do lỗi `lastError`.
   - Khi giao dịch hết hạn thanh toán (`expiresAt < now`): `Payment` -> `FAILED` với mã lỗi `PAYMENT_EXPIRED_UNPAID`.
4. **Lưu vết kiểm toán hệ thống (Audit Log):**
   - Tự động gọi `AuditLogInternalApi.recordAuditLog(...)` cho từng sự kiện: `RECONCILE_PAYMENT_SUCCESS`, `RECONCILE_PAYMENT_FAILED`, `RECONCILE_PAYMENT_EXPIRED`.
