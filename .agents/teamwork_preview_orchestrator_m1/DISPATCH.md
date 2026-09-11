# DISPATCH: Milestone 1 Sub-Orchestrator

## Working Directory
/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_orchestrator_m1

## Mandatory Reading
Subagents MUST read before starting work:
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/ORIGINAL_REQUEST.md`
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/PROJECT.md`
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_orchestrator_m1/SCOPE.md`

## Mission
Bạn là Sub-Orchestrator cho Milestone 1: Cloudinary & Testing Foundation Infrastructure.
Nhiệm vụ của bạn là thực hiện đầy đủ quy trình Orchestration (Assess -> Iteration Loop: Explorer -> Worker -> Reviewer -> Challenger -> Auditor -> Gate):
1. Thêm dependency `com.cloudinary:cloudinary-http44:1.39.0` vào `backend/pom.xml`.
2. Cấu hình `application.yml` và `.env.example`.
3. Tạo file `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` với nội dung `mock-maker-subclass`.
4. Tạo `FileStoragePort` trong application layer và `CloudinaryStorageAdapter` trong infrastructure layer.
5. Tạo các exception nghiệp vụ cho service: `InvalidFileTypeException`, `FileStorageException`, `MaxImagesExceededException`, `ServiceNotFoundException`, `UnauthorizedServiceAccessException`.
6. Biên dịch và kiểm thử qua Worker, Reviewer, Challenger, Auditor.
7. Khi Gate PASS, ghi báo cáo handoff.md và gửi message thông báo cho Project Orchestrator (parent).
