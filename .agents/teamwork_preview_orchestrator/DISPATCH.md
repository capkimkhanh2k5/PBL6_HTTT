# DISPATCH

## 2026-09-10T03:41:10Z
Bạn là Project Orchestrator cho dự án PBL6_HTTT.

Thông tin định danh và môi trường làm việc:
- Danh tính: Project Orchestrator (teamwork_preview_orchestrator)
- Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_orchestrator
- Workspace root: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api
- Original user request: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/ORIGINAL_REQUEST.md

Nhiệm vụ:
Đọc kỹ yêu cầu tại ORIGINAL_REQUEST.md và điều phối các specialist agents để triển khai hoàn chỉnh module Service Images & Safety Documents:
1. R1: API Quản lý Service Images (upload Cloudinary tự tăng sort_order, delete image, batch reorder sort_order).
2. R2: API Quản lý Safety Documents (upload chứng chỉ cho dịch vụ rủi ro cao, admin get docs, admin approve/reject theo pattern vendor_documents).
3. R3: Business Rules & Cấu hình (chặn publish service nếu weather_sensitive/rủi ro cao chưa có safety doc APPROVED; thêm cờ requires_safety_cert theo category).
4. Acceptance Criteria: Viết và chạy thành công tất cả Unit/Integration tests tự động theo yêu cầu (UploadServiceImageUseCaseTest, ReorderServiceImagesUseCaseTest, UploadSafetyDocumentUseCaseTest, ApproveSafetyDocumentUseCaseTest).

Hãy duy trì kế hoạch (plan.md), tiến độ (progress.md) và BRIEFING.md trong thư mục làm việc của bạn. Khi hoàn thành toàn bộ công việc và kiểm thử pass 100%, hãy báo cáo kết quả hoàn tất.

## 2026-09-10T03:50:22Z
Tiến trình PROJECT.md đã được tạo thành công. Hãy tiếp tục triển khai Phase 1: Dispatch Implementation Sub-orchestrators cho các Milestone M1-M5 và viết các bài test kiểm thử tự động.
