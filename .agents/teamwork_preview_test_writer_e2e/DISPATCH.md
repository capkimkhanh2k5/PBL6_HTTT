# DISPATCH: Test Writer (E2E & Acceptance Criteria Test Suite)

## Working Directory
/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_test_writer_e2e

## Mandatory Reading
Subagents MUST read before starting work:
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/ORIGINAL_REQUEST.md`
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/PROJECT.md`
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_survey_3/analysis.md`

## Mission
Bạn là Test Writer phụ trách thiết kế hạ tầng kiểm thử và xây dựng các bộ test Acceptance Criteria cho dự án PBL6_HTTT:
1. **Thiết lập TEST_INFRA.md**:
   - Viết file `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/TEST_INFRA.md` tuân thủ E2E Testing Track Principles (Tiers 1-4).
2. **Thiết kế & Soạn thảo các Acceptance Criteria Test Suites**:
   - `UploadServiceImageUseCaseTest`: Bao gồm case upload thành công (sort_order tự tăng), case vượt quá số ảnh tối đa (bị chặn), định dạng file không hợp lệ (trả về 400).
   - `ReorderServiceImagesUseCaseTest`: Bao gồm case đổi thứ tự thành công, case thao túng truyền imageId của service khác (trả về 403/404).
   - `UploadSafetyDocumentUseCaseTest`: Upload certificate tài liệu an toàn, kiểm tra phân quyền vendor, trạng thái PENDING ban đầu.
   - `ApproveSafetyDocumentUseCaseTest`: Admin approve/reject tài liệu, audit trail (reviewedBy, reviewedAt), và kiểm thử logic publish service dựa trên trạng thái của safety document và cờ category `requires_safety_cert`.
   - Vị trí các test: `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/`.
3. **Tuân thủ Clean Architecture**:
   - Sử dụng JUnit 5 Jupiter và Mockito để viết Pure Unit Test (không load Spring context để tốc độ test < 1s).
4. **Báo cáo**:
   - Sau khi hoàn thành việc viết tests và `TEST_INFRA.md`, tạo `TEST_READY.md` tại project root và ghi báo cáo vào `handoff.md`, sau đó gửi message cho Orchestrator.

## 2026-09-10T03:50:49Z
<USER_REQUEST>
Bạn là Test Writer phụ trách xây dựng hạ tầng kiểm thử và các Acceptance Criteria test suites.
Thư mục làm việc: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_test_writer_e2e
Đọc kỹ DISPATCH.md trong thư mục làm việc của bạn và thực hiện:
1. Tạo TEST_INFRA.md tại project root.
2. Thiết kế và viết các bộ test Acceptance Criteria: UploadServiceImageUseCaseTest, ReorderServiceImagesUseCaseTest, UploadSafetyDocumentUseCaseTest, ApproveSafetyDocumentUseCaseTest.
3. Khi hoàn tất, tạo TEST_READY.md tại project root, ghi handoff.md và gửi send_message cho Orchestrator.
</USER_REQUEST>

