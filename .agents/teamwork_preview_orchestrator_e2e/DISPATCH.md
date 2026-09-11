# DISPATCH: E2E Testing Orchestrator

## Working Directory
/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_orchestrator_e2e

## Mandatory Reading
Subagents MUST read before starting work:
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/ORIGINAL_REQUEST.md`
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/PROJECT.md`

## Mission
Bạn là E2E Testing Orchestrator (Dual Track - E2E Testing Track) cho dự án PBL6_HTTT.
Nhiệm vụ:
1. Tạo `TEST_INFRA.md` tại thư mục gốc dự án (`/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/TEST_INFRA.md`) theo chuẩn E2E Testing Track Principles.
2. Thiết kế và điều phối tạo các bài test Acceptance Criteria theo yêu cầu:
   - `UploadServiceImageUseCaseTest` (case upload thành công sort_order tự tăng, case vượt quá số ảnh tối đa bị chặn, case định dạng file không hợp lệ trả về 400).
   - `ReorderServiceImagesUseCaseTest` (case đổi thứ tự thành công, case thao túng truyền imageId của service khác trả về 403/404).
   - `UploadSafetyDocumentUseCaseTest` & `ApproveSafetyDocumentUseCaseTest` (tuân thủ pattern vendor_documents, cover logic publish service dựa trên trạng thái safety doc và cờ category requires_safety_cert).
   - Các kịch bản kiểm thử Tiers 1-4.
3. Khi hoàn tất toàn bộ bộ test và hạ tầng kiểm thử, tạo file `TEST_READY.md` tại thư mục gốc dự án và gửi thông báo hoàn tất về cho Project Orchestrator.
