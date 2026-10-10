# Project: Customer - Vendor Communication & Dispute Lifecycle Management Ecosystem

## Architecture
Hệ thống Tin nhắn Trao đổi Khách hàng - Đối tác (Communication) và Vòng đời Tranh chấp (Dispute Lifecycle) được tổ chức theo mô hình Modular Monolith kết hợp Clean Architecture & Domain-Driven Design (DDD) trong Backend Spring Boot (Java 21):

- **Package `com.danasea.backend.modules.communication`**:
  - `domain`: `Conversation`, `Message`, các exceptions `ConversationNotFoundException`, `UnauthorizedChatAccessException`.
  - `application`:
    - `usecases`: `CreateOrGetConversationUseCase`, `GetConversationsUseCase`, `GetMessagesUseCase`, `SendMessageUseCase`, `MarkMessagesAsReadUseCase`.
    - `dtos`: `CreateConversationRequest`, `SendMessageRequest`, `ConversationResponse`, `MessageResponse`.
  - `infrastructure`:
    - `persistence`: `ConversationJpaEntity`, `MessageJpaEntity`, `JpaConversationRepository`, `JpaMessageRepository`, mappers.
  - `presentation`:
    - `controllers`: `ConversationController` (`/api/conversations`).
    - `dtos`: Request & Response records.
    - `handlers`: `CommunicationExceptionHandler` (@RestControllerAdvice).

- **Package `com.danasea.backend.modules.dispute`**:
  - `domain`:
    - `models`: `Dispute`, `DisputeReason`, `DisputeStatus`, `DisputeResolution`.
    - `exceptions`: `DisputeNotFoundException`, `UnauthorizedDisputeAccessException`, `InvalidDisputeStateException`, `DuplicateDisputeException`.
  - `application`:
    - `usecases`:
      - Hiện hữu: `CreateDisputeUseCase`, `GetDisputesUseCase`, `ResolveDisputeUseCase`.
      - Mới: `GetCustomerDisputesUseCase`, `GetCustomerDisputeDetailUseCase`, `GetVendorDisputesUseCase`, `GetVendorDisputeDetailUseCase`, `SubmitVendorDisputeResponseUseCase`, `GetAdminDisputeDetailUseCase`, `UploadDisputeEvidenceUseCase`.
    - `dtos`: `SubmitVendorResponseRequest`, `DisputeResponse`.
  - `infrastructure`:
    - `persistence`: `DisputeJpaEntity` (bổ sung `vendor_response`, `vendor_evidence_urls`, `vendor_responded_at`), `JpaDisputeRepository`.
  - `presentation`:
    - `controllers`:
      - Cập nhật `CustomerDisputeController` (`/api/disputes`, `/api/orders/{id}/disputes`).
      - Tạo mới `VendorDisputeController` (`/api/vendor/disputes`).
      - Cập nhật `AdminDisputeController` (`/api/admin/disputes`).
      - Tạo mới `DisputeEvidenceController` (`/api/disputes/evidence`).
    - `handlers`: `DisputeExceptionHandler`.

- **Package `com.danasea.backend.modules.service.application.ports.FileStoragePort` & `FileSignatureValidator`**:
  - Tái sử dụng `FileStoragePort` (Cloudinary) và `FileSignatureValidator` (magic bytes check) cho luồng upload bằng chứng, kiểm tra MIME (JPEG, PNG, WEBP, PDF) và dung lượng tối đa 5MB.

---

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| F1 | Database Migration V25 & JPA Entities | Tạo migration `V25__add_vendor_dispute_response_and_chat_enhancements.sql` (bổ sung cột `vendor_response`, `vendor_evidence_urls`, `vendor_responded_at` vào bảng `disputes`, mở rộng `messages.content` sang `TEXT`). Cập nhật `DisputeJpaEntity`, `ConversationJpaEntity`, `MessageJpaEntity`. | M1 | Survey |
| F2 | JPA Repository Query Methods | Mở rộng `JpaConversationRepository`, `JpaMessageRepository`, và `JpaDisputeRepository` với các phương thức truy vấn tối ưu cho Customer, Vendor, MasterOrder, và REST polling. | M1 | Survey |
| F3 | Conversation Creation & List API | Triển khai `POST /api/conversations` (tạo hoặc lấy hội thoại theo `masterOrderId`) và `GET /api/conversations` (lấy danh sách hội thoại của người dùng đăng nhập kèm tin nhắn mới nhất, phân trang). | M2 | R1 |
| F4 | Message Send & REST Polling API | Triển khai `GET /api/conversations/{id}/messages` (REST polling theo `afterSequence` hoặc phân trang, sắp xếp theo sequence tăng dần; giữ `after` để tương thích) và `POST /api/conversations/{id}/messages` (gửi tin nhắn mới). Hỗ trợ đánh dấu đã đọc `isRead`. | M2 | R1 |
| F5 | Messaging RBAC & Order Ownership Verification | Kiểm soát chặt chẽ quyền truy cập hội thoại: Chỉ Customer sở hữu đơn hoặc Vendor của sub-order liên quan mới được phép xem/nhắn tin (HTTP 403 nếu vi phạm). | M2 | R1 |
| F6 | Customer Dispute Tracking API | Triển khai `GET /api/disputes` và `GET /api/disputes/{id}` cho Customer. Đảm bảo Customer Isolation: chỉ xem được tranh chấp do chính mình tạo (HTTP 403 nếu IDOR). | M3 | R2 |
| F7 | Vendor Dispute Review & Response API | Triển khai `GET /api/vendor/disputes` (lọc theo `status`), `GET /api/vendor/disputes/{id}`, và `POST /api/vendor/disputes/{id}/responses` (gửi giải trình và danh sách URL bằng chứng). Đảm bảo Vendor Isolation qua `VendorInternalApi` (HTTP 403 nếu vi phạm). | M3 | R3 |
| F8 | Admin Comprehensive Dispute Dossier API | Triển khai `GET /api/admin/disputes/{id}`: Xem toàn bộ hồ sơ tranh chấp chi tiết gồm nội dung khiếu nại của Customer, bằng chứng Customer, giải trình và bằng chứng của Vendor, và dòng thời gian trạng thái. | M3 | R4 |
| F9 | Controlled Evidence Upload & File Validation | Triển khai `POST /api/disputes/evidence`: Tải lên tệp bằng chứng qua `FileStoragePort`, kiểm tra nghiêm ngặt định dạng MIME (JPEG, PNG, WEBP, PDF), kiểm tra magic bytes, dung lượng <= 5MB. Yêu cầu xác thực. | M3 | R5 |
| F10 | API Inventory Guard Synchronization & Test Migration | Cập nhật số lượng endpoint trong `BackendApplicationTests.allApplicationEndpointsAreDiscoverable()` tương ứng với các endpoint mới (163 endpoint từ 55 controller), cập nhật `PostgreSqlMigrationIntegrationTest` lên version "26". | M4 | Survey |
| F11 | Full Test Suite Verification (100% Pass) | Toàn bộ Unit Tests và Integration Tests (MockMvc) cho các Use Cases và Controllers mới (`ConversationControllerTest`, `MessageControllerTest`, `CustomerDisputeControllerTest`, `VendorDisputeControllerTest`, `AdminDisputeControllerTest`, `DisputeEvidenceUploadTest`) chạy `./mvnw test` đạt 100% BUILD SUCCESS. | M4 | AC |

---

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Database Migration & Persistence Layer | Tạo migration Flyway V25 và V26, cập nhật `DisputeJpaEntity`, `ConversationJpaEntity`, `MessageJpaEntity`, viết query methods cho `JpaConversationRepository`, `JpaMessageRepository`, `JpaDisputeRepository`. | None | DONE |
| M2 | Customer - Vendor Messaging API (Module Communication) | Cài đặt Use Cases (`CreateOrGetConversationUseCase`, `GetConversationsUseCase`, `GetMessagesUseCase`, `SendMessageUseCase`, `MarkMessagesAsReadUseCase`), DTOs, `ConversationController` (`/api/conversations`), xử lý RBAC & Order Ownership. Viết Unit & MockMvc Tests. | M1 | DONE |
| M3 | Multi-Role Dispute Lifecycle & Controlled Evidence Upload | Cài đặt Use Cases cho Customer (`GetCustomerDisputesUseCase`, `GetCustomerDisputeDetailUseCase`), Vendor (`GetVendorDisputesUseCase`, `GetVendorDisputeDetailUseCase`, `SubmitVendorDisputeResponseUseCase`), Admin (`GetAdminDisputeDetailUseCase`), và Evidence Upload (`UploadDisputeEvidenceUseCase`). Cập nhật `CustomerDisputeController`, `AdminDisputeController`, tạo mới `VendorDisputeController` và `DisputeEvidenceController` (`POST /api/disputes/evidence` với MIME check, magic bytes, <= 5MB). Viết Unit & MockMvc Tests. | M1 | DONE |
| M4 | E2E Testing, Inventory Guard Sync & Final Verification | Cập nhật `BackendApplicationTests` (163 endpoints) & `PostgreSqlMigrationIntegrationTest` (v26). Chạy kiểm thử toàn diện `./mvnw test` trong thư mục backend đạt 100% PASS không lỗi hồi quy. Forensic audit kiểm tra tính toàn vẹn. | M1, M2, M3 | DONE |

---

## Interface Contracts
### 1. Communication API Endpoints
- `POST /api/conversations`:
  - Body: `{ "masterOrderId": "UUID", "vendorId": "UUID (required for multi-vendor orders)" }`
  - Headers: `Authorization: Bearer <token>`
  - Response: `201 Created` hoặc `200 OK` -> `ConversationResponse(id, masterOrderId, customerId, vendorId, createdAt, updatedAt, lastMessage)`
  - Errors: 400 Bad Request (`INVALID_INPUT`), 403 Forbidden (`ACCESS_DENIED`), 404 Not Found (`ORDER_NOT_FOUND`).
- `GET /api/conversations`:
  - Params: `page` (default 0), `size` (default 20, max 100)
  - Response: `200 OK` -> `Page<ConversationResponse>`
- `GET /api/conversations/{id}/messages`:
  - Params: `afterSequence` (non-negative Long, preferred polling cursor), `after` (legacy timestamp, mutually exclusive with afterSequence), `page`, `size` (default 50, max 100)
  - Response: `200 OK` -> `List<MessageResponse>` (sắp xếp sequence ASC, chỉ đánh dấu đã đọc các tin đến trong batch trả về)
  - Errors: 403 Forbidden (`ACCESS_DENIED`), 404 Not Found (`CONVERSATION_NOT_FOUND`).
- `POST /api/conversations/{id}/messages`:
  - Body: `{ "content": "String", "attachmentUrl": "String (optional)" }`
  - Response: `201 Created` -> `MessageResponse(id, conversationId, senderId, content, attachmentUrl, isRead, createdAt, sequence)`
  - Errors: 400 Bad Request (`INVALID_INPUT`), 403 Forbidden (`ACCESS_DENIED`), 404 Not Found (`CONVERSATION_NOT_FOUND`).

### 2. Dispute API Endpoints
- `GET /api/disputes`:
  - Params: `page`, `size`
  - Response: `200 OK` -> `Page<DisputeResponse>` (chỉ gồm các dispute của current user)
- `GET /api/disputes/{id}`:
  - Response: `200 OK` -> `DisputeResponse` (chứa đầy đủ thông tin giải trình của Vendor nếu có, trạng thái, bằng chứng, quyết định admin)
  - Errors: 403 Forbidden (`ACCESS_DENIED`), 404 Not Found (`DISPUTE_NOT_FOUND`).
- `GET /api/vendor/disputes`:
  - Params: `status` (optional `DisputeStatus`), `page`, `size`
  - Headers: `Authorization: Bearer <vendor_token>`
  - Response: `200 OK` -> `Page<DisputeResponse>` (chỉ gồm các dispute thuộc `sub_orders` của current vendor)
- `GET /api/vendor/disputes/{id}`:
  - Response: `200 OK` -> `DisputeResponse`
  - Errors: 403 Forbidden (`ACCESS_DENIED`), 404 Not Found (`DISPUTE_NOT_FOUND`).
- `POST /api/vendor/disputes/{id}/responses`:
  - Body: `{ "response": "String (giải trình)", "evidenceUrls": ["String"] }`
  - Response: `200 OK` -> `DisputeResponse` (cập nhật `vendorResponse`, `vendorEvidenceUrls`, `vendorRespondedAt`)
  - Errors: 400 Bad Request, 403 Forbidden (`ACCESS_DENIED`), 404 Not Found (`DISPUTE_NOT_FOUND`), 409 Conflict (nếu dispute đã giải quyết).
- `GET /api/admin/disputes/{id}`:
  - Headers: `Authorization: Bearer <admin_token>`
  - Response: `200 OK` -> `DisputeResponse` (hồ sơ toàn diện).

### 3. Evidence Upload API Endpoint
- `POST /api/disputes/evidence`:
  - Content-Type: `multipart/form-data`
  - Part: `file` (MultipartFile)
  - Validation: MIME `image/jpeg`, `image/png`, `image/webp`, `application/pdf`; size <= 5MB.
  - Response: `200 OK` -> `{ "fileUrl": "https://res.cloudinary.com/..." }`
  - Errors: 400 Bad Request (`INVALID_FILE_TYPE`), 401 Unauthorized (`UNAUTHORIZED`).

---

## Code Layout
- `backend/src/main/resources/db/migration/`:
  - `V25__add_vendor_dispute_response_and_chat_enhancements.sql`
- `backend/src/main/java/com/danasea/backend/modules/communication/`:
  - `domain/models/`: `Conversation.java`, `Message.java`
  - `domain/exceptions/`: `ConversationNotFoundException.java`, `UnauthorizedChatAccessException.java`
  - `application/dtos/`: `CreateConversationRequest.java`, `SendMessageRequest.java`, `ConversationResponse.java`, `MessageResponse.java`
  - `application/usecases/`:
    - `CreateOrGetConversationUseCase.java`
    - `GetConversationsUseCase.java`
    - `GetMessagesUseCase.java`
    - `SendMessageUseCase.java`
    - `MarkMessagesAsReadUseCase.java`
  - `infrastructure/persistence/`:
    - `entities/ConversationJpaEntity.java`, `MessageJpaEntity.java`
    - `repositories/JpaConversationRepository.java`, `JpaMessageRepository.java`
  - `presentation/controllers/`: `ConversationController.java`
  - `presentation/handlers/`: `CommunicationExceptionHandler.java`
- `backend/src/main/java/com/danasea/backend/modules/dispute/`:
  - `application/dtos/`: `SubmitVendorResponseRequest.java`, `DisputeResponse.java`
  - `application/usecases/`:
    - `GetCustomerDisputesUseCase.java`
    - `GetCustomerDisputeDetailUseCase.java`
    - `GetVendorDisputesUseCase.java`
    - `GetVendorDisputeDetailUseCase.java`
    - `SubmitVendorDisputeResponseUseCase.java`
    - `GetAdminDisputeDetailUseCase.java`
    - `UploadDisputeEvidenceUseCase.java`
  - `infrastructure/persistence/entities/`: `DisputeJpaEntity.java`
  - `presentation/controllers/`:
    - `CustomerDisputeController.java`
    - `VendorDisputeController.java`
    - `AdminDisputeController.java`
    - `DisputeEvidenceController.java`
- `backend/src/test/java/com/danasea/backend/`:
  - `BackendApplicationTests.java` (Inventory guard)
  - `infrastructure/database/PostgreSqlMigrationIntegrationTest.java`
  - `modules/communication/presentation/controllers/`:
    - `ConversationControllerTest.java`
    - `MessageControllerTest.java`
  - `modules/dispute/presentation/controllers/`:
    - `CustomerDisputeControllerTest.java`
    - `VendorDisputeControllerTest.java`
    - `AdminDisputeControllerTest.java`
    - `DisputeEvidenceUploadTest.java`

## Concurrency and polling fixes — 09/10/2026

- Creation locks the master order; a unique customer/vendor/masterOrder key prevents duplicate conversations.
- Sending locks the conversation until commit and assigns the next per-conversation sequence. Responses include persisted timestamps and sequence.
- Polling uses afterSequence and bounded batches (maximum 100), marking only delivered incoming messages as read. Clients advance their cursor after processing the whole polling batch, not from an outgoing send response.
- Legacy after resolves an exact stored timestamp to its earliest sequence; timestamp ties may replay boundary messages, which legacy clients must deduplicate by id. Use afterSequence for reliable batch progress.
- Vendor responses share the dispute write lock with admin resolution. Admin list and resolve retain all vendor explanation fields.
- V26 merges historical duplicate conversations without deleting messages and backfills ordered sequences.
- Real PostgreSQL regression tests: ChatDisputeConcurrencyIntegrationTest and ChatMigrationIntegrationTest. Full-suite verification is recorded in backend/docs/danasea-api-tracking.md after execution.
