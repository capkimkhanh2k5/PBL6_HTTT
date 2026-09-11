# BRIEFING — 2026-09-10T03:48:00Z

## Mission
Khảo sát và phân tích toàn diện tầng API và Security hiện có trong codebase để làm cơ sở triển khai module Services (Vendor CRUD & Admin Approve/Reject).

## 🔒 My Identity
- Archetype: Teamwork explorer
- Roles: survey explorer, API & Security investigation
- Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_services_module/.agents/explorer_survey_2
- Original parent: 2ade334c-a73c-4f56-992b-e52fbd610647
- Milestone: Survey & Architecture Discovery

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Write only to own folder: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_services_module/.agents/explorer_survey_2
- Adhere to Handoff Protocol (5 components: Observation, Logic Chain, Caveats, Conclusion, Verification Method)
- Respond in Vietnamese according to user rules

## Current Parent
- Conversation ID: 2ade334c-a73c-4f56-992b-e52fbd610647
- Updated: 2026-09-10T03:48:00Z

## Investigation State
- **Explored paths**:
  - `backend/docs/Clean_Architecture_Rules.md`
  - `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`
  - `backend/src/main/java/com/danasea/backend/config/ApplicationBeans.java`
  - `backend/src/main/java/com/danasea/backend/security/authentication/**`
  - `backend/src/main/java/com/danasea/backend/security/authorization/**`
  - `backend/src/main/java/com/danasea/backend/shared/**`
  - `backend/src/main/java/com/danasea/backend/modules/account/**`
  - `backend/src/main/java/com/danasea/backend/modules/vendor/**`
  - `backend/src/main/java/com/danasea/backend/modules/service/**`
  - `backend/src/test/**`
- **Key findings**:
  - Kiến trúc Clean Architecture với quy tắc tách biệt nghiêm ngặt: Use Cases là pure Java classes được khởi tạo trong các Bean configuration classes (`ApplicationBeans`, `AuthorizationBeans`).
  - Spring Security dùng stateless JWT với `JwtAuthenticationFilter`, principal được set là `user.email()`, authorities là `ROLE_CUSTOMER`, `ROLE_VENDOR`, `ROLE_ADMIN` cộng với permissions.
  - Phân quyền endpoint dùng `@PreAuthorize("hasRole('VENDOR')")` và `@PreAuthorize("hasRole('ADMIN')")`.
  - Chưa có custom CurrentUser resolver hay `@AuthenticationPrincipal` resolver; controllers nhận `Principal` (với `principal.getName()` = email).
  - Model `AuditLog` và repository `JpaAuditLogRepository` đã có trong `account` module, nhưng chưa có `AuditLogService` hay API công khai để ghi log từ module khác.
  - Error Response chuẩn qua record `ErrorResponse(String code, String message)`; đã có handler cho 400 (`INVALID_INPUT`, `INVALID_OTP`), 401 (`INVALID_CREDENTIALS`), 403 (`ACCESS_DENIED`), nhưng chưa có 404 handler chung (`ResourceNotFoundException`).
- **Unexplored areas**: None. Toàn bộ phạm vi khảo sát API & Security đã được làm rõ và xác minh bằng test build.

## Key Decisions Made
- Đã chạy verify các test hiện có bằng `./mvnw test` (với BypassSandbox để ByteBuddy Mockito attach được trên macOS sandbox).
- Biên soạn đầy đủ báo cáo bàn giao vào `handoff.md`.

## Artifact Index
- DISPATCH.md — incoming dispatch records
- BRIEFING.md — persistent working memory
- progress.md — liveness heartbeat
- handoff.md — final 5-component handoff report
