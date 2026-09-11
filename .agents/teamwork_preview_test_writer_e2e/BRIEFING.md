# BRIEFING — 2026-09-10T10:51:00+07:00

## Mission
Thiết lập hạ tầng kiểm thử TEST_INFRA.md và thiết kế, hiện thực các bộ Unit Test Acceptance Criteria cho các usecase Service Assets (UploadServiceImage, ReorderServiceImages, UploadSafetyDocument, ApproveSafetyDocument) theo chuẩn Clean Architecture (JUnit 5 + Mockito, không load Spring context, tốc độ < 1s). Sau khi hoàn tất xuất bản TEST_READY.md.

## 🔒 My Identity
- Archetype: test_writer
- Roles: specialist, qa
- Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_test_writer_e2e
- Original parent: 3cf76b9c-3d3a-4b20-bda9-91109ce9df1c
- Milestone: Service Assets Testing (Images & Safety Documents)

## 🔒 Key Constraints
- Chỉ viết và chỉnh sửa mã kiểm thử (test code only) và tài liệu kiểm thử (TEST_INFRA.md, TEST_READY.md) - TUYỆT ĐỐI không sửa code implementation.
- Sử dụng JUnit 5 Jupiter + Mockito cho Pure Unit Test, chạy nhanh (<1s), độc lập và không phụ thuộc Spring context.
- Kiểm thử bao quát Acceptance Criteria: Upload ảnh, Reorder ảnh, Upload tài liệu an toàn, Duyệt/Từ chối tài liệu và Publish service.
- Không đặt source code hay test trong `.agents/`. Vị trí test: `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/`.
- Toàn bộ giao tiếp bằng tiếng Việt.

## Current Parent
- Conversation ID: 3cf76b9c-3d3a-4b20-bda9-91109ce9df1c
- Updated: not yet

## Task Summary
- **What to build**:
  1. `TEST_INFRA.md` tại project root (E2E testing principles, test tiers, test strategies).
  2. Test suites: `UploadServiceImageUseCaseTest`, `ReorderServiceImagesUseCaseTest`, `UploadSafetyDocumentUseCaseTest`, `ApproveSafetyDocumentUseCaseTest`.
  3. `TEST_READY.md` tại project root.
  4. `handoff.md` trong thư mục agent.
- **Success criteria**: All tests compile and pass via target test command.
- **Interface contracts**: `PROJECT.md`, `ORIGINAL_REQUEST.md`, `analysis.md`.
- **Code layout**: `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/`.

## Key Decisions Made
- Dùng MockitoExtension và Pure Unit Test để đảm bảo test tốc độ cao và cô lập hoàn toàn business logic.

## Artifact Index
- `TEST_INFRA.md` — Project root: Test infrastructure documentation
- `TEST_READY.md` — Project root: Test readiness declaration
- `handoff.md` — Agent directory: Complete handoff report
