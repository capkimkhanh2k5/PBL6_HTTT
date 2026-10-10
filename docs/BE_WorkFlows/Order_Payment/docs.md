# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE ORDER & PAYMENT (ĐƠN HÀNG & THANH TOÁN)

> Module Order & Payment chịu trách nhiệm tạo Master Order, tự động tách Sub-Orders 1:1 theo từng nhà cung cấp (Vendor), tính toán tỷ lệ hoa hồng nền tảng (Commission Policy), tích hợp thanh toán đa cổng (VNPay, PayPal) và xử lý Webhook IPN hoàn tất giao dịch an toàn, chống giả mạo chữ ký số và bảo vệ chống xử lý lặp (Idempotency).

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `Order_Creation_And_Splitting_DF` | Decision Flowchart | [`codeFlows/Decision_Flowchart/Order_Creation_And_Splitting_DF.mmd`](./codeFlows/Decision_Flowchart/Order_Creation_And_Splitting_DF.mmd) — Tạo Master Order từ Booking HOLD: kiểm tra Idempotency, xác thực IDOR, tách Sub-Orders 1:1, áp dụng hoa hồng và voucher |
| 2 | `Payment_Intent_DualGateways_DF` | Decision Flowchart | [`codeFlows/Decision_Flowchart/Payment_Intent_DualGateways_DF.mmd`](./codeFlows/Decision_Flowchart/Payment_Intent_DualGateways_DF.mmd) — Khởi tạo Intent đa cổng, Payment Waiver Guard chặn thanh toán thiếu cam kết, phân luồng VNPay vs PayPal |
| 3 | `Payment_Webhook_And_Fulfillment_DF` | Decision Flowchart | [`codeFlows/Decision_Flowchart/Payment_Webhook_And_Fulfillment_DF.mmd`](./codeFlows/Decision_Flowchart/Payment_Webhook_And_Fulfillment_DF.mmd) — Xử lý Webhook IPN: xác thực chữ ký VNPay/PayPal, Idempotency Guard, hoàn tất đơn PAID và xác nhận Booking |
| 4 | `Order_Payment_Lifecycle_SM` | State Machine | [`codeFlows/State_Machine/Order_Payment_Lifecycle_SM.mmd`](./codeFlows/State_Machine/Order_Payment_Lifecycle_SM.mmd) — Máy trạng thái 3 trục: Vòng đời đơn (`OrderStatus`), Trục dòng tiền (`PaymentStatus`), Đơn phụ (`SubOrderStatus`) |
| 5 | `EPIC04_Architecture_And_UseCases_ARCH` | Flowchart | Kiến trúc phân lớp Clean Architecture tách biệt các Use Cases: Tạo đơn, Đa cổng thanh toán, Webhook, Xem trước mức hoàn tiền và Khởi tạo hoàn tiền |
| 6 | `Admin_MultiFilter_Listing_SD` | Sequence Diagram | [`codeFlows/Sequence_Diagram/Admin_MultiFilter_Listing_SD.mmd`](./codeFlows/Sequence_Diagram/Admin_MultiFilter_Listing_SD.mmd) — Quản trị toàn sàn: Lọc đa chiều đơn hàng, giao dịch thanh toán và hoàn tiền cho Admin |
| 7 | `Automated_Payment_Reconciliation_SD` | Sequence & Flowchart | [`codeFlows/Sequence_Diagram/Automated_Payment_Reconciliation_SD.mmd`](./codeFlows/Sequence_Diagram/Automated_Payment_Reconciliation_SD.mmd) & [`codeFlows/State_Machine/Reconciliation_Decision_SM.mmd`](./codeFlows/State_Machine/Reconciliation_Decision_SM.mmd) — Đối soát tự động định kỳ |
| 8 | `Order_Receipt_And_Pdf_Generation_SD` | Sequence Diagram | [`codeFlows/Sequence_Diagram/Order_Receipt_And_Pdf_Generation_SD.mmd`](./codeFlows/Sequence_Diagram/Order_Receipt_And_Pdf_Generation_SD.mmd) — Phát hành biên nhận đơn hàng (`GET /api/orders/{id}/receipt`): IDOR, điều kiện PAID, xuất PDF |
| 9 | `Sub_Order_Waiver_Acceptance_And_Payment_Guard_SD` | 4 Decision Flowcharts + 1 Sequence | [`codeFlows/Decision_Flowchart/Waiver_Payment_Guard_DF.mmd`](codeFlows/Decision_Flowchart/Waiver_Payment_Guard_DF.mmd) & [`codeFlows/Sequence_Diagram/Sub_Order_Waiver_Acceptance_And_Payment_Guard_SD.mmd`](codeFlows/Sequence_Diagram/Sub_Order_Waiver_Acceptance_And_Payment_Guard_SD.mmd) — Cam kết an toàn trước thanh toán |

---

### 1. Order_Creation_And_Splitting_DF.png — Tạo Master Order & Tách Sub-Orders 1:1

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

## 2. Payment_Intent_DualGateways_DF.png — Tạo Payment Intent Đa Cổng, Khóa Tuần Tự & Bền Vững Hóa

**Luồng nghiệp vụ chi tiết:**
1. Khách hàng gửi yêu cầu thanh toán qua `POST /api/payments/{orderId}/create-intent` kèm `{provider: "VNPAY" | "PAYPAL"}`.
2. **Kiểm tra quyền sở hữu & trạng thái đơn:**
   - Kiểm tra IDOR: `order.customerId == currentUserId` (403 Forbidden nếu không phải chủ đơn).
   - Đơn hàng bắt buộc phải ở trạng thái `PENDING_PAYMENT` (409 Conflict nếu đơn đã thanh toán hoặc đã hủy).
   - **Bảo vệ cam kết an toàn (Payment Waiver Guard - R9):** Khóa các `SubOrder` thuộc đơn hàng (`ORDER BY s.id ASC`), kiểm tra xem có SubOrder nào yêu cầu cam kết an toàn (`waiverRequired = true`) nhưng chưa được khách xác nhận (`waiverAccepted != true`) hay không. Nếu có, lập tức ném `WaiverAcceptanceRequiredException` trả về HTTP 409 Conflict với code `WAIVER_ACCEPTANCE_REQUIRED` và danh sách `missingSubOrders`, ngăn chặn tuyệt đối việc tạo payment intent hoặc gọi sang cổng thanh toán.
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
   - **Payment Waiver Guard Phase 2:** Thực hiện kiểm tra toàn bộ các SubOrder bắt buộc tương tự trước khi gọi PayPal API capture.
   - `OrderPaymentService` gọi PayPal REST API v2 `POST /v2/checkout/orders/{id}/capture` để khớp dòng tiền, lưu `providerCaptureId` và cập nhật đơn hàng thành `PAID`.
6. **Đối soát tự động (Payment Reconciliation Job):**
   - `PaymentReconciliationJob` chạy ngầm định kỳ quét các `Payment` ở trạng thái `PENDING` quá thời gian quy định (ví dụ quá 15 phút) để tự động truy vấn trạng thái từ cổng thanh toán và đồng bộ dữ liệu.
7. **Quản trị thanh toán (Admin Payment Management):**
   - `GET /api/admin/payments`: Xem danh sách toàn bộ các thanh toán trong hệ thống (phân trang, lọc theo trạng thái).
   - `GET /api/admin/payments/{id}`: Tra cứu chi tiết thanh toán, mã tham chiếu cổng, lịch sử capture và snapshot giao dịch.

---

## 3. Payment_Webhook_And_Fulfillment_DF.png — Xử Lý Webhook IPN & Xác Nhận Đơn Hàng

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

## 4. Order_Payment_Lifecycle_SM.png — Vòng Đời Trạng Thái Đơn Hàng & Dòng Tiền

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

## 5. EPIC04_Architecture_And_UseCases_ARCH.png — Kiến Trúc Clean Architecture Phân Tách Use Cases

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

---

## 8. Order_Receipt_And_Pdf_Generation_SD.mmd — Phát Hành Biên Nhận Đơn Hàng & Xuất PDF

**Lớp xử lý chính:**
- `com.danasea.backend.modules.order.presentation.controllers.OrderController`
- `com.danasea.backend.modules.order.application.usecases.GetOrderReceiptUseCase`
- `com.danasea.backend.modules.order.infrastructure.pdf.PdfReceiptGenerator`

**Điểm truy cập API:** `GET /api/orders/{id}/receipt` (Yêu cầu JWT xác thực)
- Query param: `format=json` (mặc định) hoặc `format=pdf`.
- Header: `Accept: application/pdf` hoặc `Accept: application/json`.

**Quy tắc nghiệp vụ & Bảo mật (R6):**
1. **Kiểm tra quyền sở hữu (IDOR Guard):** Khách hàng chỉ được phép tra cứu và xuất biên nhận cho đơn hàng do chính mình sở hữu (`order.customerId == currentUserId`). Quản trị viên (`isAdmin = true`) có toàn quyền tra cứu biên nhận cho mọi đơn hàng. Vi phạm trả về `403 Forbidden` (`UnauthorizedOrderAccessException`).
2. **Điều kiện phát hành (Paid Status Guard):** Biên nhận chỉ được cấp cho đơn hàng đã thanh toán thành công (`paymentStatus != UNPAID`). Đơn hàng chưa thanh toán (`UNPAID`) hoặc bị hủy khi chưa thanh toán sẽ bị từ chối với `400 Bad Request` (`UnpaidOrderReceiptException`).
3. **Lắp ráp dữ liệu hóa đơn chi tiết:**
   - Sinh mã tra cứu: `orderCode` (`ORD-XXXXXXXX`) và `receiptCode` (`REC-XXXXXXXX`).
   - Nạp danh sách đơn con (`SubOrders`), tên dịch vụ bản địa hóa qua `LocalizedContentSelector` (Việt / Anh), số lượng, đơn giá, giảm giá voucher và thành tiền thực thu.
   - Thông tin phương thức thanh toán (`VNPAY`, `PAYPAL`) và mốc thời gian giao dịch thực tế `paidAt`.
4. **Bộ sinh PDF tiếng Việt chuyên dụng (Apache PDFBox 3.0.8):**
   - Tích hợp phông chữ `NotoSans-Regular.ttf` và `NotoSans-Bold.ttf` hỗ trợ hiển thị đầy đủ dấu tiếng Việt Unicode mà không bị lỗi phông hay ký tự lạ.
   - Cơ chế ngắt dòng tự động (Word Wrapping) và phân trang động (`LINES_PER_PAGE = 43`), hỗ trợ các đơn hàng có nhiều mục dịch vụ trải dài qua nhiều trang mà không bị tràn khung.
   - Trả về nhị phân `application/pdf` kèm header `Content-Disposition: inline; filename="receipt-{orderId}.pdf"`.

---

## 9. Sub_Order_Waiver_Acceptance_And_Payment_Guard — Cam Kết An Toàn Trước Thanh Toán & Guard Bảo Vệ

> 📌 *Hệ thống sơ đồ Decision Flowchart & Sequence Diagrams:*
> - Sơ đồ 1: Luồng tổng quan khách hàng & kiểm soát cam kết: [`codeFlows/Decision_Flowchart/Waiver_Customer_Journey_DF.mmd`](./codeFlows/Decision_Flowchart/Waiver_Customer_Journey_DF.mmd)
> - Sơ đồ 2: Chính sách Vendor, Khóa bi quan & Quản lý phiên bản: [`codeFlows/Decision_Flowchart/Waiver_Vendor_Policy_DF.mmd`](./codeFlows/Decision_Flowchart/Waiver_Vendor_Policy_DF.mmd)
> - Sơ đồ 3: Các tầng bảo vệ khi xác nhận cam kết (Verification Gates): [`codeFlows/Decision_Flowchart/Waiver_Acceptance_Verification_DF.mmd`](./codeFlows/Decision_Flowchart/Waiver_Acceptance_Verification_DF.mmd)
> - Sơ đồ 4: Cơ chế Payment Waiver Guard (Chặn Create Intent & PayPal Capture): [`codeFlows/Decision_Flowchart/Waiver_Payment_Guard_DF.mmd`](./codeFlows/Decision_Flowchart/Waiver_Payment_Guard_DF.mmd)
> - Sơ đồ tuần tự hợp nhất: [`codeFlows/Sequence_Diagram/Sub_Order_Waiver_Acceptance_And_Payment_Guard_SD.mmd`](./codeFlows/Sequence_Diagram/Sub_Order_Waiver_Acceptance_And_Payment_Guard_SD.mmd)

**Lớp xử lý chính:**
- `com.danasea.backend.modules.order.presentation.controllers.SubOrderWaiverController`
- `com.danasea.backend.modules.order.application.usecases.AcceptSubOrderWaiverUseCase`
- `com.danasea.backend.modules.order.application.services.WaiverAcceptanceGuard`
- `com.danasea.backend.modules.order.application.usecases.CreatePaymentIntentUseCase`
- `com.danasea.backend.modules.order.application.OrderPaymentService`
- `com.danasea.backend.modules.order.domain.ports.ServiceWaiverLookupPort`
- `com.danasea.backend.shared.i18n.LocalizedContentSelector`
- `com.danasea.backend.modules.service.presentation.controllers.VendorServiceController`
- `com.danasea.backend.modules.service.application.usecases.CreateServiceUseCase`
- `com.danasea.backend.modules.service.application.usecases.UpdateServiceUseCase`
- `com.danasea.backend.modules.audit.application.api.AuditLogInternalApi`

### Điểm truy cập API: `POST /api/sub-orders/{id}/waiver-acceptance`
- **Yêu cầu bảo mật:** Bắt buộc JWT xác thực với quyền `ROLE_CUSTOMER`.
- **Request Body:**
  ```json
  {
    "accepted": true,
    "version": 1,
    "language": "VI"
  }
  ```
- **Validation (Ánh xạ đa ngôn ngữ i18n):**
  - `accepted`: `@NotNull(message = "{validation.accepted_flag_is_required}")`, `@AssertTrue(message = "{validation.safety_waiver_must_be_accepted}")`.
  - `version`: `@NotNull(message = "{validation.waiver_version_is_required}")`.
  - `language`: `@NotBlank(message = "{validation.language_is_required}")`, chuẩn hóa ("VI" hoặc "EN").

### Quy tắc nghiệp vụ then chốt:

1. **Chính sách tại Service & Khóa Tuần Tự Hóa (Vendor Policy & Row Lock):**
   - Quản lý chính sách cam kết an toàn thông qua `waiverRequired`, `waiverVersion`, `waiverContent`, `waiverContentEn`.
   - **Khóa bi quan ngăn Race Condition:** `UpdateServiceUseCase` được bảo vệ bởi `@Transactional` kết hợp `serviceRepository.findByIdForUpdate(cmd.serviceId())`. Các yêu cầu cập nhật đồng thời từ Vendor sẽ được hàng đợi DB tuần tự hóa, sinh các `waiverVersion` riêng biệt liên tiếp (v1 $\rightarrow$ v2 $\rightarrow$ v3) mà không bị mất dữ liệu cập nhật (Lost Update).
   - **Bảo vệ nội dung:** Cấm bật `waiverRequired = true` khi cả nội dung tiếng Việt và tiếng Anh đều để trống (`WaiverContentRequiredException` $\rightarrow$ `400 Bad Request`).
   - **Độc lập với thời tiết:** Thuộc tính `waiverRequired` hoàn toàn độc lập với `weatherSensitive`, không tự động suy diễn từ nhau.
   - **Tự động quản lý Version (Dirty Check):** Khởi tạo `waiverVersion = 1`. Khi vendor cập nhật dịch vụ:
     - Chuẩn hóa chuỗi (loại bỏ khoảng trắng thừa).
     - Chỉ tăng `waiverVersion = waiverVersion + 1` một lần khi thực sự có thay đổi ở `waiverRequired` hoặc nội dung VI/EN.
     - Thay đổi giá bán, tiêu đề, ảnh,... không làm tăng `waiverVersion`.

2. **Snapshot bất biến theo Đơn hàng (Order Waiver Snapshot):**
   - Khi tạo đơn hàng (áp dụng cho cả `CreateOrderUseCase` và `OrderPaymentService.createOrder`):
     - Tra cứu thông qua `ServiceWaiverLookupPort` và ghi nhận snapshot bất biến từ Service vào từng `SubOrder`: `waiverRequired`, `waiverVersion`, `waiverContent`, `waiverContentEn`.
     - Vendor sửa đổi dịch vụ sau này chỉ áp dụng cho đơn mới; toàn bộ đơn cũ giữ nguyên snapshot cam kết tại thời điểm đặt.
   - **Hợp đồng đọc (Public Detail & Order Detail):** Trả về đầy đủ trạng thái `waiverRequired`, `waiverVersion`, nội dung snapshot theo ngôn ngữ yêu cầu (`LocalizedContentSelector`), ngôn ngữ thực tế trả về và cờ `fallbackUsed`.

3. **Cơ chế Khóa Bi Quan & Concurrency (Pessimistic Lock Order):**
   - Để triệt tiêu xung đột dữ liệu (Race Condition) và Deadlock giữa luồng khách chấp thuận cam kết và luồng bấm thanh toán/capture:
     - Luôn khóa `MasterOrder` trước bằng `findByIdForUpdate`.
     - Sau đó khóa các `SubOrder` theo thứ tự ID tăng dần (`ORDER BY s.id ASC`).
   - API xác nhận cam kết chỉ giữ khóa trong transaction DB ngắn. Luồng payment hiện có commit bản ghi PENDING trước khi gọi cổng, nhưng transaction khởi tạo/capture vẫn giữ khóa Payment/MasterOrder trong lúc gọi gateway để tuần tự hóa retry; không coi đây là luồng không giữ khóa qua mạng.

4. **Xác thực API Chấp thuận Cam kết (`AcceptSubOrderWaiverUseCase`):**
   - **IDOR Guard:** Kiểm tra `masterOrder.customerId == currentUserId`. Nếu vi phạm $\rightarrow$ **HTTP 403 Forbidden** (`UnauthorizedOrderAccessException`).
   - **State Guard:** Đơn hàng bắt buộc phải ở trạng thái `PENDING_PAYMENT` và SubOrder ở trạng thái `PENDING`. Vi phạm $\rightarrow$ **HTTP 409 Conflict** (`InvalidOrderStateException`).
   - **Required Guard:** SubOrder bắt buộc phải có `waiverRequired == true`. Nếu dịch vụ không yêu cầu cam kết $\rightarrow$ **HTTP 409 Conflict** (`InvalidOrderStateException`).
   - **Version Mismatch Guard:** `command.version() == subOrder.getWaiverVersion()`. Nếu sai lệch $\rightarrow$ **HTTP 409 Conflict** (`WaiverVersionMismatchException`).
   - **Bằng chứng xác nhận đáng tin cậy:** Tuyệt đối không nhận `userId`, thời điểm xác nhận hay nội dung từ client khai báo. Toàn bộ thông tin được ghi nhận tự động từ:
     - `waiverAcceptedBy`: Lấy từ JWT Security Context (`currentUserId`).
     - `waiverAcceptedAt`: Lấy từ đồng hồ chuẩn của Server (`OffsetDateTime.now()`).
     - `waiverAcceptedContent`: Lấy từ nội dung snapshot của SubOrder tương ứng với ngôn ngữ đã chọn (kèm logic fallback VI/EN).
   - **Tính Idempotent (Replay cùng xác nhận):** Nếu SubOrder đã được xác nhận trước đó với cùng `version` và cùng `userId`:
     - Trả về kết quả xác nhận đã lưu (`200 OK`).
     - **Không ghi đè lại timestamp** `waiverAcceptedAt`.
     - **Không sinh log kiểm toán lặp** (Audit Log).

5. **Payment Waiver Guard & Phản hồi HTTP 409 Conflict:**
   - **Thành phần Guard tập trung (`WaiverAcceptanceGuard`):** Cả `CreatePaymentIntentUseCase` và `OrderPaymentService.capturePayPalOrder` đều thống nhất gọi qua phương thức tĩnh `WaiverAcceptanceGuard.missing(...)` để kiểm tra điều kiện cam kết của từng SubOrder.
   - Trước khi tạo Payment Intent hoặc Capture PayPal:
     - Quét toàn bộ các SubOrder của MasterOrder: nếu có bất kỳ SubOrder nào có `waiverRequired = true` và `waiverAccepted != true`:
     - Ném `WaiverAcceptanceRequiredException` và **ngừng ngay lập tức luồng thanh toán mà không gọi sang cổng thanh toán (VNPay / PayPal)**.
     - `OrderExceptionHandler` ánh xạ sang **HTTP 409 Conflict** với cấu trúc chi tiết:
       ```json
       {
         "code": "WAIVER_ACCEPTANCE_REQUIRED",
         "message": "Một hoặc nhiều dịch vụ yêu cầu xác nhận cam kết an toàn trước khi thanh toán.",
         "orderId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
         "missingSubOrders": [
           {
             "subOrderId": "c2b3d4e5-...",
             "serviceId": "a1b2c3d4-...",
             "serviceName": "Tour Lặn Biển Cù Lao Chàm",
             "waiverVersion": 1,
             "required": true,
             "waiverContent": "Khách hàng cam kết đủ điều kiện sức khỏe và tuân thủ hướng dẫn an toàn...",
             "contentLanguage": "VI",
             "fallbackUsed": false
           }
         ]
       }
       ```
   - Frontend có thể sử dụng ngay dữ liệu trong `missingSubOrders` để mở popup/modal cam kết cho khách hàng đọc và bấm đồng ý mà không cần gọi thêm API truy vấn phụ.
