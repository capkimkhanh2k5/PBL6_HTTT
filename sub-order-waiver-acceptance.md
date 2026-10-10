# Kế hoạch triển khai: Cam kết an toàn trước thanh toán (Sub-Order Waiver Acceptance)

## 1. Mục tiêu và phạm vi
- **Mục tiêu:** Ghi nhận xác nhận của người đặt đối với đúng nội dung cam kết của từng sub-order bắt buộc, trước khi khởi tạo thanh toán.
- **Luồng nghiệp vụ:** Giữ chỗ (Hold) -> Tạo order (chưa thanh toán - `PENDING_PAYMENT`) -> Đọc/xác nhận cam kết (`POST /api/sub-orders/{id}/waiver-acceptance`) -> Khởi tạo thanh toán (`createPaymentIntent` / capture).
- **Nguyên tắc then chốt:**
  1. Vendor chỉ sửa dịch vụ của mình; không bật `waiverRequired` nếu nội dung trống; không tự suy ra yêu cầu từ `weatherSensitive`.
  2. Quản lý `waiverVersion` tự động: mặc định là 1; tăng version khi thực sự đổi `waiverRequired` hoặc nội dung VI/EN (sau chuẩn hóa dirty check). Đổi giá/ảnh không tăng.
  3. Snapshot theo Order: Cả `CreateOrderUseCase` và `OrderPaymentService` lưu snapshot (`waiverRequired`, `waiverVersion`, `waiverContent`, `waiverContentEn`) tại thời điểm tạo đơn. Vendor sửa dịch vụ sau này không ảnh hưởng đơn cũ.
  4. Hợp đồng đọc: Public detail và Order detail trả đủ trạng thái required, version, content, language và fallback flag.
  5. API xác nhận: `POST /api/sub-orders/{id}/waiver-acceptance` nhận `accepted: true`, `version`, `language`. Kiểm tra chủ sở hữu JWT, trạng thái `PENDING_PAYMENT`, version/language đối chiếu snapshot. Idempotent replay không đổi timestamp/audit.
  6. Guard thanh toán: Chặn thanh toán/capture nếu thiếu cam kết bắt buộc; trả HTTP 409 Conflict với code `WAIVER_ACCEPTANCE_REQUIRED` và danh sách sub-orders thiếu. DB `PESSIMISTIC_WRITE` khóa `MasterOrder` rồi các `SubOrder` theo thứ tự ID để bảo vệ concurrency; API xác nhận chỉ giữ khóa trong DB; luồng payment/capture hiện có vẫn giữ khóa Payment/MasterOrder trong transaction gọi gateway để tuần tự hóa retry.
  7. Migration Flyway `V35`: Backfill snapshot cho các `SubOrder` PENDING thuộc `MasterOrder` có trạng thái `PENDING_PAYMENT`. Đơn đã trả tiền giữ nguyên lịch sử, không giả lập `waiverAccepted=true`. Webhook và đối soát vẫn ghi nhận kết quả từ cổng.

---

## 2. Thiết kế chi tiết & Thay đổi các tầng

### 2.1. Cơ sở dữ liệu (Migration `V35__sub_order_waiver_acceptance.sql`)
1. **Bảng `services`:**
   - Đảm bảo `waiver_content` và `waiver_content_en` là `TEXT`.
   - Bổ sung `waiver_required BOOLEAN NOT NULL DEFAULT FALSE`.
   - Bổ sung `waiver_version INTEGER NOT NULL DEFAULT 1`.
2. **Bảng `sub_orders`:**
   - Bổ sung `waiver_required BOOLEAN NOT NULL DEFAULT FALSE`.
   - Bổ sung `waiver_version INTEGER NOT NULL DEFAULT 1`.
   - Bổ sung `waiver_content TEXT`.
   - Bổ sung `waiver_content_en TEXT`.
   - Bổ sung `waiver_accepted_by UUID`.
   - Bổ sung `waiver_accepted_language VARCHAR(10)`.
   - Bổ sung `waiver_accepted_content TEXT`.
   - (`waiver_accepted` và `waiver_accepted_at` đã có sẵn từ V1/V7/V12).
3. **Data Backfill:**
   - Backfill snapshot từ `services` sang `sub_orders` cho các sub_orders thuộc master orders có `status = 'PENDING_PAYMENT'`:
     `waiver_required = s.waiver_required`, `waiver_version = s.waiver_version`, `waiver_content = s.waiver_content`, `waiver_content_en = s.waiver_content_en`.
   - Đối với đơn cũ khác (`PAID`, `COMPLETED`, `CANCELLED`): `waiver_required` giữ `FALSE`, không giả lập `waiver_accepted = true`.

### 2.2. Module Service (Quản lý chính sách cam kết)
1. **Entities & Models:**
   - Cập nhật `ServiceJpaEntity`: `@Column(columnDefinition = "TEXT") private String waiverContent;`, `waiverRequired`, `waiverVersion`.
   - Cập nhật `Service`: `waiverRequired`, `waiverVersion`.
2. **DTOs & Mappers:**
   - `CreateServiceCommand`, `CreateServiceRequest`: thêm `waiverRequired`.
   - `UpdateServiceCommand`, `UpdateServiceRequest`: thêm `waiverRequired`.
   - `ServiceResult`, `ServiceDetailResponse`, `ServiceDetailResult`: thêm `waiverRequired`, `waiverVersion`, `waiverContent`, `waiverLanguage`, `waiverFallbackUsed`.
3. **Logic UseCases:**
   - `CreateServiceUseCase`: Mặc định `waiverVersion = 1`. Nếu `waiverRequired == true` mà cả `waiverContent` và `waiverContentEn` đều rỗng -> ném lỗi `WaiverContentRequiredException`.
   - `UpdateServiceUseCase`: Kiểm tra quyền sở hữu vendor. Dirty check: Chuẩn hóa chuỗi (trim). Nếu có thay đổi ở `waiverRequired`, `waiverContent`, hoặc `waiverContentEn`:
     - Kiểm tra nếu `waiverRequired == true` mà nội dung rỗng -> ném lỗi.
     - Tăng `waiverVersion = waiverVersion + 1`.
     - Nếu chỉ đổi giá, tên, ảnh,... -> `waiverVersion` giữ nguyên.
   - `GetPublicServiceDetailUseCase`: Trả `waiverRequired`, `waiverVersion`, `waiverContent`, `waiverLanguage` ("VI" hoặc "EN"), `waiverFallbackUsed` (boolean).

### 2.3. Module Order - Snapshot theo đơn hàng
1. **Entities & Models:**
   - Cập nhật `SubOrderJpaEntity` & `SubOrder`:
     - Snapshot: `waiverRequired`, `waiverVersion`, `waiverContent`, `waiverContentEn`.
     - Acceptance: `waiverAccepted`, `waiverAcceptedAt`, `waiverAcceptedBy`, `waiverAcceptedLanguage`, `waiverAcceptedContent`.
2. **Tạo đơn hàng:**
   - `CreateOrderUseCase`: Đọc thông tin service thông qua `ServiceLookupPort` (hoặc mở rộng `BookingOrderView` / JPA) để ghi snapshot vào `SubOrder`.
   - `OrderPaymentService.createOrder`: Tương tự, lấy thông tin service để ghi snapshot vào `SubOrderJpaEntity`.
3. **Hiển thị chi tiết đơn hàng:**
   - Cập nhật `SubOrderResponse`: Trả các trường `waiverRequired`, `waiverVersion`, `waiverContent`, `waiverLanguage`, `waiverFallbackUsed`, `waiverAccepted`, `waiverAcceptedAt`, `waiverAcceptedBy`, `waiverAcceptedLanguage`.

### 2.4. Module Order - API xác nhận cam kết
1. **Endpoint:** `POST /api/sub-orders/{id}/waiver-acceptance`
2. **Request Body:**
   ```json
   {
     "accepted": true,
     "version": 1,
     "language": "VI"
   }
   ```
3. **Xử lý UseCase (`AcceptSubOrderWaiverUseCase`):**
   - Lấy `userId` từ JWT `SecurityUtils.getCurrentUserId()`.
   - Bắt đầu transaction ngắn:
     - Khóa `MasterOrder` bằng `PESSIMISTIC_WRITE`.
     - Khóa `SubOrder` bằng `PESSIMISTIC_WRITE`.
     - Kiểm tra chủ sở hữu: `masterOrder.getCustomerId().equals(userId)`. Nếu sai -> `403 Forbidden` (`UnauthorizedOrderAccessException`).
     - Kiểm tra trạng thái đơn: `masterOrder.getStatus() == MasterOrderStatus.PENDING_PAYMENT`. Nếu sai -> `409 Conflict` (`InvalidOrderStateException`).
     - Kiểm tra `waiverRequired`: nếu không yêu cầu, có thể ghi nhận hoặc trả thành công ngay.
     - Kiểm tra `version`: `request.version().equals(subOrder.getWaiverVersion())`. Nếu sai -> `409 Conflict` (`WaiverVersionMismatchException`).
     - Kiểm tra `language` và lấy nội dung snapshot tương ứng:
       - Nếu language = "EN": ưu tiên `subOrder.getWaiverContentEn()`, nếu rỗng dùng `subOrder.getWaiverContent()` (fallback).
       - Nếu language = "VI": ưu tiên `subOrder.getWaiverContent()`, nếu rỗng dùng `subOrder.getWaiverContentEn()`.
     - **Replay / Idempotency:** Nếu `subOrder.getWaiverAccepted() == true` và cùng `version` / cùng `acceptedBy`:
       - Trả về thông tin acceptance đã lưu, KHÔNG cập nhật lại `waiverAcceptedAt`, KHÔNG ghi audit lặp.
     - **Xác nhận mới:**
       - Ghi `waiverAccepted = true`, `waiverAcceptedAt = OffsetDateTime.now()`, `waiverAcceptedBy = userId`, `waiverAcceptedLanguage = resolvedLang`, `waiverAcceptedContent = agreedContent`.
       - Lưu SubOrder và ghi AuditLog / Domain Event.
4. **Response Body:**
   ```json
   {
     "subOrderId": "...",
     "masterOrderId": "...",
     "accepted": true,
     "waiverVersion": 1,
     "language": "VI",
     "acceptedAt": "2026-10-10T14:00:00Z"
   }
   ```

### 2.5. Module Order - Payment Guard & Concurrency
1. **Cơ chế Guard:**
   - Trong `CreatePaymentIntentUseCase` (trước khi gọi cổng thanh toán VNPay/PayPal):
     - `preparePayment`: Khóa `MasterOrder` (đã có `findByIdForUpdate`).
     - Khóa các `SubOrder` theo `masterOrderId` sắp xếp theo ID tăng dần (`ORDER BY id ASC`).
     - Lọc các `subOrder` có `waiverRequired == true && !Boolean.TRUE.equals(waiverAccepted)`.
     - Nếu danh sách còn thiếu: Ném `WaiverAcceptanceRequiredException(orderId, missingSubOrders)`.
   - Trong `OrderPaymentService.capturePayPalOrder`:
     - Kiểm tra tương tự trước khi thực hiện gọi PayPal capture.
2. **Phản hồi lỗi 409 Conflict:**
   - `OrderExceptionHandler` xử lý `WaiverAcceptanceRequiredException`:
   - HTTP 409 Conflict với cấu trúc:
     ```json
     {
       "code": "WAIVER_ACCEPTANCE_REQUIRED",
       "message": "Một hoặc nhiều dịch vụ yêu cầu xác nhận cam kết an toàn trước khi thanh toán.",
       "orderId": "UUID",
       "missingSubOrders": [
         {
           "subOrderId": "UUID",
           "serviceId": "UUID",
           "serviceName": "...",
           "waiverVersion": 1,
           "required": true,
           "waiverContent": "...",
           "contentLanguage": "VI",
           "fallbackUsed": false
         }
       ]
     }
     ```
3. **Đảm bảo không gọi cổng:** Khi exception xảy ra, transaction rollback / abort trước bất kỳ network call nào đến cổng thanh toán.

---

## 3. Các bước triển khai (Task Breakdown)

1. [x] **Bước 1: Thiết kế & Kế hoạch (`sub-order-waiver-acceptance.md`)**
2. [x] **Bước 2: Flyway Migration V35**
   - Viết `V35__sub_order_waiver_acceptance.sql` bổ sung cột cho `services`, `sub_orders` và backfill dữ liệu cho đơn PENDING.
3. [x] **Bước 3: Mở rộng Service Module**
   - Entity `ServiceJpaEntity`, Domain `Service`.
   - DTOs `CreateServiceRequest`, `UpdateServiceRequest`, `ServiceDetailResponse`,...
   - UseCases: `CreateServiceUseCase`, `UpdateServiceUseCase` (dirty check tăng version), `GetPublicServiceDetailUseCase`.
   - i18n keys trong `common_vi.properties`, `common_en.properties`.
4. [x] **Bước 4: Mở rộng Order Module & Snapshot**
   - Entity `SubOrderJpaEntity`, Domain `SubOrder`.
   - Thêm `ServiceWaiverLookupPort` / `ServiceWaiverLookupAdapter` tích hợp tra cứu snapshot service vào `CreateOrderUseCase` và `OrderPaymentService`.
   - Cập nhật `SubOrderResponse` và mapper.
5. [x] **Bước 5: Xây dựng API Acceptance**
   - DTOs `SubOrderWaiverAcceptanceRequest`, `SubOrderWaiverAcceptanceResponse`.
   - UseCase `AcceptSubOrderWaiverUseCase` với DB pessimistic lock, kiểm tra owner, kiểm tra trạng thái, snapshot matching, idempotent replay.
   - Controller `SubOrderWaiverController` (`POST /api/sub-orders/{id}/waiver-acceptance`).
6. [x] **Bước 6: Xây dựng Payment Guard**
   - Exception `WaiverAcceptanceRequiredException` và response DTO `WaiverAcceptanceRequiredResponse`.
   - Trích xuất component tập trung `WaiverAcceptanceGuard.missing(...)` thẩm định điều kiện cam kết của từng SubOrder, nạp tên dịch vụ và chuẩn hóa ngôn ngữ hiển thị/fallback.
   - Thêm phương thức kiểm tra guard trong `CreatePaymentIntentUseCase` và `OrderPaymentService.capturePayPalOrder`.
   - Xử lý exception trong `OrderExceptionHandler` trả HTTP 409 Conflict với code `WAIVER_ACCEPTANCE_REQUIRED` và `missingSubOrders`.
7. [x] **Bước 7: Kiểm thử & Nghiệm thu**
   - Bộ test suite tích hợp & unit tests đạt 100% (85/85 tests tập trung PASS và 2.317 tests toàn hệ thống PASS):
     - `VendorServiceWaiverPolicyTest` (8 tests): vendor quyền sửa, không bật required khi rỗng, dirty check tăng version, weatherSensitive độc lập.
     - `CatalogPublicDetailWaiverTest` (4 tests): Public detail VI/EN, version, fallback flag.
     - `OrderWaiverSnapshotTest` (2 tests): `CreateOrderUseCase` và `OrderPaymentService` lưu snapshot bất biến.
     - `AcceptSubOrderWaiverUseCaseTest` (11 tests): đúng owner, 403 khác chủ, 409 sai status, 409 không yêu cầu waiver, 409 sai version, idempotent replay, snapshot matching, audit log.
     - `PaymentWaiverGuardTest` (4 tests): chặn `createPaymentIntent`/`capturePayPalOrder` khi thiếu acceptance, không gọi gateway, trả đủ missingSubOrders với nội dung snapshot; thanh toán thành công khi đã accept đủ.
     - `SubOrderWaiverControllerTest` (5 tests): MockMvc HTTP endpoints, validation @AssertTrue, 409 mismatch, 403 unauthenticated.
     - `OrderExceptionHandlerWaiverTest` (2 tests): Ánh xạ HTTP 409 đúng payload spec.
     - `WaiverPersistenceIntegrationTest` (4 tests E2E trên PostgreSQL/Redis): Kiểm tra đồng thời vendor update serialize tuần tự tăng version, chấp thuận song song bảo toàn 1 bằng chứng gốc, thanh toán không cam kết không chạm cổng gateway, và tuần tự hóa giữa acceptance và payment.
     - Regression tests (47 tests): `CreatePaymentIntentUseCaseTest`, `CreateOrderUseCaseTest`, `OrderControllerTest`, `UpdateServiceUseCaseTest`, `CreateServiceUseCaseTest`, `GetPublicServiceDetailUseCaseTest`, `OrderPaymentServiceTest`, `InternalPaymentWebhookProfileTest`, `I18nSourceGuardTest`.
   - Đã cập nhật tài liệu workflow và sơ đồ Mermaid 4 giai đoạn tại `docs/BE_WorkFlows/Order_Payment/`.
