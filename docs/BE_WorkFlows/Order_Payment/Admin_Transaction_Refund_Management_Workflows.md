# 📖 LUỒNG NGHIỆP VỤ & SƠ ĐỒ — QUẢN TRỊ GIAO DỊCH, THEO DÕI HOÀN TIỀN & ĐỐI SOÁT TỰ ĐỘNG
*(Admin Transaction, Refund Tracking & Payment Reconciliation Workflows)*

> **Module:** Order & Payment / Refund / Audit  
> **Nhánh phát triển:** `admin_transaction_refund_management` (P1 Requirement)  
> **Phạm vi nghiệp vụ:** Cung cấp bộ API quản trị giao dịch toàn sàn cho Quản trị viên (Admin), API lịch sử thanh toán/hoàn tiền theo đơn hàng có kiểm soát bảo mật (IDOR Guard), cơ chế tra cứu trạng thái cổng tự động (VNPay QueryDR API & PayPal Capture Query) khi webhook bị chậm hoặc thất lạc, và tự động ghi nhận nhật ký kiểm toán hệ thống (Audit Log).

---

## 📑 MỤC LỤC SƠ ĐỒ

| # | Tên sơ đồ | Loại sơ đồ | Nội dung nghiệp vụ |
|---|-----------|-----------|--------------------|
| 1 | `Admin_MultiFilter_Listing_Flow` | Sequence Diagram | Admin tra cứu và lọc đa chiều Đơn hàng, Giao dịch thanh toán và Yêu cầu hoàn tiền toàn sàn (Tối ưu hóa tránh N+1 Query, hỗ trợ lọc `vendorId` qua `EXISTS sub_orders`). |
| 2 | `Order_History_IDOR_Guard_Flow` | Sequence Diagram | Khách hàng và Admin tra cứu lịch sử thanh toán (`/payments`) và hoàn tiền (`/refunds`) của đơn hàng với cơ chế phòng vệ IDOR nghiêm ngặt (chặn 403 Forbidden nếu không phải chủ đơn). |
| 3 | `Automated_Payment_Reconciliation_Flow` | Sequence Diagram | Scheduled Background Job (`PaymentReconciliationJob`): Tự động quét giao dịch `PENDING` > 2 phút, gọi VNPay QueryDR (HMAC-SHA512) và PayPal Capture để đồng bộ trạng thái `PAID` và ghi nhận `AuditLogInternalApi`. |
| 4 | `Reconciliation_Decision_StateMachine` | State Machine / Flowchart | Cây quyết định đối soát dòng tiền đa cổng, xử lý timeout, quá hạn thanh toán (`PAYMENT_EXPIRED_UNPAID`) và chuyển đổi trạng thái liên hoàn sang `MasterOrder`, `SubOrder` và `Booking`. |

---

## 1. SƠ ĐỒ 1: QUẢN TRỊ TOÀN SÀN ĐƠN HÀNG, THANH TOÁN & HOÀN TIỀN (ADMIN LISTING)

### Mô tả nghiệp vụ
Quản trị viên cần công cụ giám sát toàn diện mọi giao dịch phát sinh trên sàn DANASEA:
- **`GET /api/admin/orders`**: Danh sách đơn hàng toàn sàn. Nhận các bộ lọc `status`, `paymentStatus`, `customerId`, `vendorId` (lọc theo đơn vị cung ứng qua bảng phụ `sub_orders`), khoảng thời gian `from`/`to` (hoặc `fromDate`/`toDate`). Trả về DTO tóm tắt tối ưu `AdminOrderSummaryResponse` (chứa `totalItems`, `vendorIds`, `totalAmount`).
- **`GET /api/admin/payments` & `GET /api/admin/payments/{id}`**: Giám sát dòng tiền thanh toán toàn sàn, lọc theo cổng thanh toán (`provider`), mã đơn (`orderId`), `vendorId`, `customerId`, thời gian.
- **`GET /api/admin/refunds` & `GET /api/admin/refunds/{id}`**: Giám sát các yêu cầu hoàn tiền, lọc theo lý do (`reason`), trạng thái (`status`), `subOrderId`, `orderId`, `vendorId`, `customerId`.

### Thiết kế tối ưu hóa CSDL (Anti-N+1 Query & Sub-query EXISTS)
- Với `vendorId` trong đơn hàng: Sử dụng mệnh đề `EXISTS (SELECT 1 FROM sub_orders so WHERE so.master_order_id = m.id AND so.vendor_id = :vendorId)`.
- Gom nhóm `SubOrder` theo lô (`findByMasterOrderIdIn(orderIds)`) trên bộ nhớ để đếm số items và trích xuất danh sách `vendorIds` mà không phát sinh truy vấn N+1 vào database.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Quản trị viên (Admin Portal)
    participant AC as Admin Controllers<br/>(AdminOrder / AdminPayment / AdminRefund)
    participant OPS as OrderPaymentService
    participant Repo as JPA Repositories<br/>(MasterOrder / Payment / Refund / SubOrder)
    participant DB as PostgreSQL Database

    Note over Admin,DB: 1. TRA CỨU ĐƠN HÀNG TOÀN SÀN (GET /api/admin/orders)
    Admin->>AC: GET /api/admin/orders?status=PENDING_PAYMENT&vendorId=...&page=0&size=20
    AC->>OPS: getAdminOrders(status, paymentStatus, customerId, vendorId, from, to, pageable)
    OPS->>Repo: JpaMasterOrderRepository.findByAdminFilters(status, paymentStatus, customerId, vendorId, from, to, pageable)
    Repo->>DB: SELECT m.* FROM master_orders m WHERE ... AND EXISTS(SELECT 1 FROM sub_orders WHERE vendor_id = :vendorId) ORDER BY created_at DESC
    DB-->>Repo: Page<MasterOrderJpaEntity>
    OPS->>Repo: JpaSubOrderRepository.findByMasterOrderIdIn(orderIds)
    Repo->>DB: SELECT * FROM sub_orders WHERE master_order_id IN (:orderIds)
    DB-->>Repo: List<SubOrderJpaEntity>
    Note over OPS: Gom nhóm vendorIds và tính totalItems theo từng orderId trên RAM (Tránh N+1)
    OPS-->>AC: Page<AdminOrderSummaryResponse>
    AC-->>Admin: HTTP 200 OK (Danh sách đơn hàng phân trang)

    Note over Admin,DB: 2. TRA CỨU GIAO DỊCH THANH TOÁN (GET /api/admin/payments)
    Admin->>AC: GET /api/admin/payments?status=SUCCESS&provider=VNPAY&vendorId=...
    AC->>OPS: getAdminPayments(status, provider, orderId, customerId, vendorId, from, to, pageable)
    OPS->>Repo: JpaPaymentRepository.findByAdvancedFilters(...)
    Repo->>DB: SELECT p.* FROM payments p WHERE ... AND EXISTS(SELECT 1 FROM sub_orders WHERE vendor_id = :vendorId)
    DB-->>Repo: Page<PaymentJpaEntity>
    OPS-->>AC: Page<PaymentResponse>
    AC-->>Admin: HTTP 200 OK (Danh sách thanh toán toàn sàn)

    Note over Admin,DB: 3. TRA CỨU YÊU CẦU HOÀN TIỀN (GET /api/admin/refunds)
    Admin->>AC: GET /api/admin/refunds?status=PROCESSED&reason=CUSTOMER_CANCEL&vendorId=...
    AC->>OPS: getAdminRefunds(status, reason, subOrderId, provider, vendorId, customerId, orderId, from, to, pageable)
    OPS->>Repo: JpaRefundRepository.findByAdvancedFilters(...)
    Repo->>DB: SELECT r.* FROM refunds r WHERE ... AND EXISTS(SELECT 1 FROM sub_orders WHERE vendor_id = :vendorId)
    DB-->>Repo: Page<RefundJpaEntity>
    OPS-->>AC: Page<RefundDetailResponse>
    AC-->>Admin: HTTP 200 OK (Danh sách hoàn tiền toàn sàn)
```

---

## 2. SƠ ĐỒ 2: LỊCH SỬ GIAO DỊCH & HOÀN TIỀN THEO ĐƠN HÀNG (IDOR GUARD FLOW)

### Mô tả nghiệp vụ
Khách hàng có nhu cầu xem lại toàn bộ lịch sử thanh toán và tiến trình hoàn tiền của đơn hàng mình đã mua:
- **`GET /api/orders/{id}/payments`**: Trả về các lần tạo intent, mã tham chiếu cổng, số tiền và trạng thái thanh toán.
- **`GET /api/orders/{id}/refunds`**: Trả về danh sách chi tiết các yêu cầu bồi hoàn/hủy chỗ gắn với từng dịch vụ con (`sub_orders`).
- **Cơ chế an ninh IDOR (Insecure Direct Object Reference):**
  - Hệ thống lấy `currentUserId` từ Spring SecurityContext.
  - So sánh đối chiếu `assertOwner(order.getCustomerId(), currentUserId)`.
  - Nếu là tài khoản khác (User B) cố tình truy cập đơn của User A: Lập tức chặn với ngoại lệ `AccessDeniedException` (**HTTP 403 Forbidden**).
  - Quản trị viên (`ROLE_ADMIN`) có quyền xem lịch sử của mọi đơn hàng phục vụ hỗ trợ giải quyết sự cố.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Khách hàng / Quản trị viên
    participant OC as OrderController
    participant OPS as OrderPaymentService
    participant MOR as JpaMasterOrderRepository
    participant PR as JpaPaymentRepository
    participant RR as JpaRefundRepository
    participant DB as PostgreSQL Database

    Client->>OC: GET /api/orders/{orderId}/payments (hoặc /refunds)
    Note over OC: Trích xuất currentUserId và quyền ROLE_ADMIN từ SecurityContext
    OC->>OPS: getOrderPayments(currentUserId, orderId, isAdmin) (hoặc getOrderRefunds)

    OPS->>MOR: findById(orderId)
    MOR->>DB: SELECT * FROM master_orders WHERE id = :orderId
    alt Không tìm thấy đơn hàng
        DB-->>MOR: Optional.empty()
        MOR-->>OPS: empty
        OPS-->>OC: Ném OrderNotFoundException
        OC-->>Client: HTTP 404 Not Found
    else Tìm thấy đơn hàng
        DB-->>MOR: MasterOrderJpaEntity
        MOR-->>OPS: order

        Note over OPS: BẢO VỆ QUYỀN SỞ HỮU (IDOR GUARD CHECK)
        alt !isAdmin VÀ order.customerId != currentUserId
            Note over OPS: Người dùng khác cố tình đọc trộm đơn hàng
            OPS-->>OC: Ném AccessDeniedException("User does not have permission")
            OC-->>Client: HTTP 403 Forbidden (Từ chối truy cập)
        else Hợp lệ: Là chủ đơn hàng (Owner) HOẶC là Quản trị viên (Admin)
            alt Yêu cầu tra cứu Payments
                OPS->>PR: findByMasterOrderIdOrderByCreatedAtDesc(orderId)
                PR->>DB: SELECT * FROM payments WHERE master_order_id = :orderId ORDER BY created_at DESC
                DB-->>PR: List<PaymentJpaEntity>
                PR-->>OPS: payments
                OPS-->>OC: List<PaymentResponse>
                OC-->>Client: HTTP 200 OK (Danh sách lịch sử thanh toán)
            else Yêu cầu tra cứu Refunds
                OPS->>RR: findByMasterOrderId(orderId)
                RR->>DB: SELECT r.* FROM refunds r JOIN sub_orders s ON r.sub_order_id = s.id WHERE s.master_order_id = :orderId
                DB-->>RR: List<RefundJpaEntity>
                RR-->>OPS: refunds
                OPS-->>OC: List<RefundDetailResponse>
                OC-->>Client: HTTP 200 OK (Danh sách lịch sử hoàn tiền)
            end
        end
    end
```

---

## 3. SƠ ĐỒ 3: TỰ ĐỘNG ĐỐI SOÁT CỔNG THANH TOÁN (PAYMENT RECONCILIATION JOB)

### Mô tả nghiệp vụ
Trong các kịch bản mạng thực tế, Webhook IPN từ cổng thanh toán có thể bị thất lạc, drop mạng, hoặc đến chậm sau nhiều phút. Nếu không có cơ chế đối soát:
- Khách hàng đã bị trừ tiền ở ngân hàng/cổng nhưng đơn hàng trên hệ thống DANASEA vẫn ở trạng thái `PENDING_PAYMENT`.
- Chỗ giữ (Booking HOLD) có thể bị hết hạn oan uổng.

**Giải pháp:** Scheduled Job ngầm `PaymentReconciliationJob` chạy mỗi 60 giây:
1. **Quét giao dịch quá hạn:** Quét các payment `PENDING` tạo trước mốc 2 phút (`createdAt < now - 2m`).
2. **Chủ động tra soát cổng (QueryDR):**
   - **VNPay:** Gọi VNPay QueryDR API với `vnp_Command=querydr`, tạo chữ ký checksum HMAC-SHA512 đối chiếu mã đơn hàng và ngày giao dịch `providerTransactionDate`.
   - **PayPal:** Tra cứu trạng thái Order/Capture v2 từ cổng PayPal.
3. **Đồng bộ liên hoàn nguyên tử:**
   - Nếu cổng báo `COMPLETED`: Cập nhật `Payment` -> `SUCCESS`, `MasterOrder` -> `PAID`, `SubOrder` -> `CONFIRMED`, hoàn tất `Booking` -> `CONFIRMED`.
   - Nếu cổng báo `FAILED`: Cập nhật `Payment` -> `FAILED` kèm lý do lỗi.
   - Nếu quá hạn thanh toán (`expiresAt < now`): Cập nhật `Payment` -> `FAILED` (`PAYMENT_EXPIRED_UNPAID`).
4. **Lưu vết kiểm toán hệ thống:** Gọi `AuditLogInternalApi` ghi nhận chi tiết hành động đối soát (`RECONCILE_PAYMENT_SUCCESS`, `RECONCILE_PAYMENT_FAILED`, `RECONCILE_PAYMENT_EXPIRED`).

```mermaid
sequenceDiagram
    autonumber
    participant Job as PaymentReconciliationJob<br/>(@Scheduled every 60s)
    participant OPS as OrderPaymentService
    participant Port as PaymentGatewayPort<br/>(VNPay / PayPal Adapters)
    participant Gateway as Cổng thanh toán bên ngoài<br/>(VNPay Sandbox / PayPal API v2)
    participant Repo as DB Repositories<br/>(Payment / MasterOrder / SubOrder)
    participant UseCase as ConfirmBookingUseCase
    participant Audit as AuditLogInternalApi<br/>(Audit Module)

    Job->>Repo: JpaPaymentRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(PENDING, now - 2m)
    Repo-->>Job: List<PaymentJpaEntity> (Các thanh toán pending chưa nhận được webhook)

    loop Xử lý từng giao dịch (Bọc try-catch độc lập)
        Job->>OPS: reconcilePayment(paymentId)

        alt Cổng là VNPay (provider == VNPAY)
            OPS->>Port: queryPayment(VNPAY, providerOrderId, providerTransactionDate)
            Note over Port: VNPayPaymentAdapter: Tạo request querydr,<br/>băm HMAC-SHA512(vnp_HashSecret)
            Port->>Gateway: POST https://sandbox.vnpayment.vn/merchant_webapi/api/transaction (querydr)
            Gateway-->>Port: JSON: vnp_ResponseCode, vnp_TransactionStatus, vnp_TransactionNo

            alt Giao dịch Thành công (ResponseCode="00" & TransactionStatus="00")
                Port-->>OPS: PaymentCaptureResult(success=true, status="COMPLETED", captureId=vnp_TransactionNo)
                Note over OPS: ĐỒNG BỘ TRẠNG THÁI LIÊN HOÀN (FULL FULFILLMENT)
                OPS->>Repo: Payment.setStatus(SUCCESS), setProviderTransactionId(captureId)
                OPS->>Repo: MasterOrder.setStatus(PAID)
                OPS->>Repo: SubOrders.setStatus(CONFIRMED)
                OPS->>UseCase: execute(ConfirmBookingCommand(bookingId, customerId))
                Note over UseCase: Chuyển Booking sang CONFIRMED
                OPS->>Audit: recordAuditLog(null, "RECONCILE_PAYMENT_SUCCESS", "PAYMENT", paymentId, metadataJson)
            else Giao dịch Thất bại tại ngân hàng / Khách hủy (TransactionStatus != "00")
                Port-->>OPS: PaymentCaptureResult(success=false, status="FAILED", message="...")
                OPS->>Repo: Payment.setStatus(FAILED), setLastError("VNPAY_STATUS_FAILED: ...")
                OPS->>Audit: recordAuditLog(null, "RECONCILE_PAYMENT_FAILED", "PAYMENT", paymentId, metadataJson)
            end

        else Cổng là PayPal (provider == PAYPAL)
            OPS->>Port: queryPayment(PAYPAL, providerOrderId, null)
            Port->>Gateway: GET https://api-m.sandbox.paypal.com/v2/checkout/orders/{id}
            Gateway-->>Port: PayPal Order Details: status ("COMPLETED" / "VOIDED")

            alt PayPal đã COMPLETED
                Port-->>OPS: PaymentCaptureResult(success=true, status="COMPLETED", captureId=...)
                OPS->>Repo: Payment.setStatus(SUCCESS), MasterOrder.setStatus(PAID), SubOrders.setStatus(CONFIRMED)
                OPS->>UseCase: execute(ConfirmBookingCommand)
                OPS->>Audit: recordAuditLog(null, "RECONCILE_PAYMENT_SUCCESS", "PAYMENT", paymentId, metadataJson)
            else PayPal bị hủy / VOIDED
                Port-->>OPS: PaymentCaptureResult(success=false, status="VOIDED")
                OPS->>Repo: Payment.setStatus(FAILED), setLastError("PAYPAL_ORDER_VOIDED")
                OPS->>Audit: recordAuditLog(null, "RECONCILE_PAYMENT_FAILED", "PAYMENT", paymentId, metadataJson)
            end

        else Giao dịch PENDING đã quá hạn (expiresAt < now)
            Note over OPS: Quá thời hạn giữ chỗ thanh toán
            OPS->>Repo: Payment.setStatus(FAILED), setLastError("PAYMENT_EXPIRED_UNPAID")
            OPS->>Audit: recordAuditLog(null, "RECONCILE_PAYMENT_EXPIRED", "PAYMENT", paymentId, metadataJson)
        end
    end
```

---

## 4. SƠ ĐỒ 4: CÂY QUYẾT ĐỊNH & MÁY TRẠNG THÁI ĐỐI SOÁT (RECONCILIATION STATE MACHINE)

Sơ đồ thể hiện quy trình phân nhánh quyết định logic đối soát dòng tiền khi hệ thống phát hiện giao dịch `PENDING` quá hạn:

```mermaid
flowchart TD
    Start(["Bắt đầu chu kỳ quét (Mỗi 60s)"]) --> ScanPending["Quét Payment trạng thái PENDING<br/>và createdAt < now - 2 phút"]
    ScanPending --> HasPending{"Có giao dịch PENDING?"}
    
    HasPending -- "Không" --> EndWait(["Nghỉ đến chu kỳ tiếp theo"])
    HasPending -- "Có" --> PickPayment["Lấy bản ghi Payment và khóa dữ liệu"]

    PickPayment --> CheckProvider{"Cổng thanh toán?"}

    %% Nhánh VNPay
    CheckProvider -- "VNPAY" --> CheckVNPayDate{"Có providerTransactionDate?"}
    CheckVNPayDate -- "Có" --> CallQueryDR["Gọi VNPay QueryDR API<br/>(HMAC-SHA512 Checksum)"]
    CallQueryDR --> ParseVNPay{"Kết quả QueryDR?"}
    
    ParseVNPay -- "vnp_ResponseCode = 00<br/>vnp_TransactionStatus = 00" --> MarkSuccess["Cập nhật Payment: SUCCESS<br/>Lưu providerTransactionId"]
    ParseVNPay -- "vnp_TransactionStatus != 00<br/>(Lỗi thẻ/Hủy giao dịch)" --> MarkFailed["Cập nhật Payment: FAILED<br/>Lưu mã lỗi cổng"]
    ParseVNPay -- "Lỗi mạng / Timeout" --> CheckExpiry
    CheckVNPayDate -- "Không có date" --> CheckExpiry

    %% Nhánh PayPal
    CheckProvider -- "PAYPAL" --> CheckPayPalCapture{"Có captureRequestId?"}
    CheckPayPalCapture -- "Có" --> ReconcileCapture["Gọi reconcilePayPalCapture()"]
    CheckPayPalCapture -- "Không" --> QueryPayPalOrder["Gọi PayPal REST API v2<br/>Tra cứu Order status"]
    QueryPayPalOrder --> ParsePayPal{"Kết quả PayPal?"}
    
    ParsePayPal -- "COMPLETED" --> MarkSuccess
    ParsePayPal -- "VOIDED / EXPIRED" --> MarkFailed
    ParsePayPal -- "Khác / Lỗi mạng" --> CheckExpiry

    %% Nhánh Kiểm tra hết hạn
    CheckExpiry{"expiresAt < now()?"}
    CheckExpiry -- "Đã hết hạn" --> MarkExpired["Cập nhật Payment: FAILED<br/>lastError: PAYMENT_EXPIRED_UNPAID"]
    CheckExpiry -- "Chưa hết hạn" --> SkipPayment(["Giữ nguyên PENDING<br/>Chờ lượt quét sau"])

    %% Hậu xử lý thành công
    MarkSuccess --> SyncOrder["MasterOrder: PAID<br/>SubOrders: CONFIRMED<br/>Booking: CONFIRMED"]
    SyncOrder --> AuditSuccess["AuditLog: RECONCILE_PAYMENT_SUCCESS"]
    AuditSuccess --> DoneItem(["Hoàn tất giao dịch"])

    %% Hậu xử lý thất bại
    MarkFailed --> AuditFailed["AuditLog: RECONCILE_PAYMENT_FAILED"]
    AuditFailed --> DoneItem

    MarkExpired --> AuditExpired["AuditLog: RECONCILE_PAYMENT_EXPIRED"]
    AuditExpired --> DoneItem
```

---

## 5. TỔNG KẾT CÁC LỚP THÀNH PHẦN MÃ NGUỒN LIÊN QUAN

| Thành phần | Đường dẫn tệp tin | Trách nhiệm chính |
| :--- | :--- | :--- |
| **Admin Order Controller** | `backend/.../modules/order/presentation/controllers/AdminOrderController.java` | Tiếp nhận `GET /api/admin/orders`, xử lý phân trang và truyền bộ lọc xuống service. |
| **Admin Payment Controller** | `backend/.../modules/order/presentation/controllers/AdminPaymentController.java` | Tiếp nhận `GET /api/admin/payments` và `GET /api/admin/payments/{id}`. |
| **Admin Refund Controller** | `backend/.../modules/order/presentation/controllers/AdminRefundController.java` | Tiếp nhận `GET /api/admin/refunds` và `GET /api/admin/refunds/{id}`. |
| **Order Controller** | `backend/.../modules/order/presentation/controllers/OrderController.java` | Tiếp nhận `GET /api/orders/{id}/payments` và `GET /api/orders/{id}/refunds` kèm IDOR Guard. |
| **Order Payment Service** | `backend/.../modules/order/application/OrderPaymentService.java` | Điều phối nghiệp vụ lọc đa bảng, gom nhóm tránh N+1, thực thi `reconcilePayment` và ghi Audit Log. |
| **Reconciliation Job** | `backend/.../modules/order/infrastructure/jobs/PaymentReconciliationJob.java` | Scheduled Job chạy định kỳ mỗi 60 giây quét các giao dịch pending treo. |
| **Payment Gateway Port** | `backend/.../modules/order/domain/ports/PaymentGatewayPort.java` | Định nghĩa giao diện `queryPayment(provider, providerOrderId, transactionDate)`. |
| **VNPay Adapter** | `backend/.../modules/order/infrastructure/adapters/VNPayPaymentAdapter.java` | Hiện thực gọi VNPay QueryDR API với băm HMAC-SHA512 checksum. |
| **PayPal Adapter** | `backend/.../modules/order/infrastructure/adapters/PayPalPaymentAdapter.java` | Hiện thực gọi PayPal Order/Capture Query qua REST API v2. |
| **Audit Internal API** | `backend/.../modules/audit/application/api/AuditLogInternalApi.java` | Lưu vết kiểm toán cho các sự kiện đối soát dòng tiền tự động. |
| **Database Migration V23** | `backend/.../resources/db/migration/V23__admin_transaction_reconciliation.sql` | Bổ sung index và cột `provider_order_id`, `provider_transaction_date` tối ưu tốc độ tra cứu. |
