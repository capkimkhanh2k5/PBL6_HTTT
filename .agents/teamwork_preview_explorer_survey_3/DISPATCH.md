# DISPATCH: Explorer Survey 3 (Service Lifecycle, Category, Publish Rules & Test Infrastructure)

## Working Directory
/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_survey_3

## Mandatory Reading
Subagents MUST read ORIGINAL_REQUEST.md before starting work:
`/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/ORIGINAL_REQUEST.md`

## Objective
Khảo sát Service Lifecycle, Category entity, Publish flow và Test Infrastructure của dự án:
1. Tìm hiểu Model/Entity `categories`: hiện tại có những trường gì? Làm sao để thêm cờ `requires_safety_cert` (migration, entity definition, seeders nếu có)?
2. Tìm hiểu luồng duyệt/publish service:
   - Khi nào service chuyển sang `PUBLISHED`? (Ví dụ: Admin approve service, Vendor publish, hoặc API publish).
   - Logic hiện tại của Service model/service use cases về `weather_sensitive` và trạng thái rủi ro cao.
   - Tìm kiếm vị trí cần chặn/kiểm tra điều kiện: Nếu `weather_sensitive=true` hoặc category có `requires_safety_cert=true` (hoặc rủi ro cao) mà chưa có safety document nào được `APPROVED` thì KHÔNG ĐƯỢC set thành `PUBLISHED`.
3. Khảo sát Test Infrastructure của dự án:
   - Framework test nào đang được sử dụng (Jest, Vitest, Mocha, Pytest, v.v.)?
   - Lệnh chạy test là gì?
   - Cấu trúc thư mục test, cách mock database/repositories/Cloudinary/use-cases trong các bài test hiện có.
   - Các file test mẫu đang viết theo phong cách nào (Unit test, Integration test, E2E test).
4. Xác định cấu trúc và yêu cầu cho Acceptance Criteria:
   - `UploadServiceImageUseCaseTest`
   - `ReorderServiceImagesUseCaseTest`
   - `UploadSafetyDocumentUseCaseTest`
   - `ApproveSafetyDocumentUseCaseTest`

## Output
Ghi báo cáo khảo sát chi tiết vào file:
`/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_survey_3/analysis.md`
Kèm theo `handoff.md` tóm tắt các phát hiện và gửi message thông báo hoàn tất cho Orchestrator.

## 2026-09-10T03:41:58Z
Bạn là Explorer khảo sát codebase về Service Lifecycle, Category, Publish flow và Test Infrastructure.
Thư mục làm việc của bạn: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_survey_3
Đọc kỹ DISPATCH.md tại thư mục làm việc và ORIGINAL_REQUEST.md tại /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/ORIGINAL_REQUEST.md.
Nhiệm vụ: Khảo sát Category entity, Publish flow logic, weather_sensitive & high-risk checks, và Test Runner/Mocking patterns theo hướng dẫn trong DISPATCH.md.
Ghi báo cáo chi tiết vào analysis.md và handoff.md trong thư mục làm việc của bạn, sau đó dùng send_message để gửi kết quả hoàn tất cho Orchestrator.
