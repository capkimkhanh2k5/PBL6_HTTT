# BRIEFING — 2026-09-10T03:48:00Z

## Mission
Khảo sát chi tiết pattern vendor_documents hiện tại và thiết kế Safety Documents cho services theo yêu cầu R2/R3.

## 🔒 My Identity
- Archetype: explorer
- Roles: [investigation, synthesis]
- Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_survey_2
- Original parent: 3cf76b9c-3d3a-4b20-bda9-91109ce9df1c
- Milestone: survey_vendor_documents_and_safety_documents

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Response completely in Vietnamese
- Strict adherence to 5-Component Handoff Protocol
- Communication via send_message to parent

## Current Parent
- Conversation ID: 3cf76b9c-3d3a-4b20-bda9-91109ce9df1c
- Updated: 2026-09-10T03:48:00Z

## Investigation State
- **Explored paths**:
  - `backend/src/main/java/com/danasea/backend/modules/vendor/**` (Vendor, VendorDocument, DocStatus, DocType, JpaVendorDocumentRepository, JpaVendorRepository)
  - `backend/src/main/java/com/danasea/backend/modules/service/**` (Service, ServiceSafetyDocument, ServiceImage, Category, ServiceStatus, DocStatus)
  - `backend/src/main/java/com/danasea/backend/security/**` (SecurityConfig, JwtAuthenticationFilter, AuthorizationController, AuthorizationAdapter)
  - `backend/src/main/java/com/danasea/backend/modules/account/**` (User, Role, AccountInternalApi)
  - `backend/docs/Clean_Architecture_Rules.md`, `backend/docs/DANASEA_Database_Design.docx`
- **Key findings**:
  - `VendorDocument` và `ServiceSafetyDocument` đã có sẵn Domain Model, JPA Entity và JPA Repository rỗng.
  - Hiện tại chưa có UseCase, DTO, Controller hay Test nào cho documents trong cả 2 module.
  - Dự án tuân thủ nghiêm ngặt Clean Architecture (Dependency Rule, Framework Independence qua Spring @Bean configs, Ports & Adapters).
  - Vòng đời trạng thái: `DocStatus` (PENDING -> APPROVED / REJECTED) kèm `reviewedBy` và `reviewedAt`. Cần bổ sung `rejectionReason`.
  - Auth: Phân quyền `@PreAuthorize("hasRole('VENDOR')")` & `@PreAuthorize("hasRole('ADMIN')")`, kết hợp Resource Ownership check (Vendor chỉ quản lý service của chính mình).
  - Nghiệp vụ R3: Thêm `requiresSafetyCert` vào `Category` và `CategoryJpaEntity`. Khi publish service, nếu `requiresSafetyCert == true` hoặc `weatherSensitive == true` thì bắt buộc phải có ít nhất 1 safety document có `status == APPROVED`.
- **Unexplored areas**: Không có, đã hoàn tất toàn bộ khảo sát theo yêu cầu.

## Key Decisions Made
- Thiết kế trọn vẹn kiến trúc Safety Documents theo Clean Architecture 4 tầng (Domain, Application, Infrastructure, Presentation) dựa trên chuẩn mực của `modules/account` và `security/authentication`.
- Ghi nhận chi tiết caveat về mockito ByteBuddy trên môi trường macOS sandbox để cảnh báo cho Implementer.

## Artifact Index
- DISPATCH.md — Chỉ đạo khảo sát
- BRIEFING.md — Working memory
- progress.md — Tiến độ thực hiện
- analysis.md — Báo cáo khảo sát và thiết kế chi tiết
- handoff.md — Báo cáo bàn giao 5 thành phần
