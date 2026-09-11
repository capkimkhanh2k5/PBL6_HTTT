# BRIEFING — 2026-09-10T11:15:50+07:00

## Mission
Investigate use case requirements, business logic, file validation, and interaction with FileStoragePort for Service Images in Milestone 2.

## 🔒 My Identity
- Archetype: explorer
- Roles: investigator, synthesizer
- Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_m2_2
- Original parent: 10eadd78-2077-480b-a859-cfd0fcb5d524
- Milestone: Milestone 2 (Application Layer & Use Cases)

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Do NOT modify or write any source code files
- BẮT BUỘC PHẢI TUÂN THEO: Trả lời cho user phải hoàn toàn bằng TIẾNG VIỆT trong toàn bộ chat

## Current Parent
- Conversation ID: 10eadd78-2077-480b-a859-cfd0fcb5d524
- Updated: 2026-09-10T04:11:30Z

## Investigation State
- **Explored paths**:
  - `backend/docs/Clean_Architecture_Rules.md`
  - `backend/src/main/java/com/danasea/backend/modules/service/application/ports/FileStoragePort.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/storage/CloudinaryStorageAdapter.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/config/ServiceInfrastructureConfig.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/domain/exceptions/` (7 exception classes)
  - `backend/src/main/java/com/danasea/backend/modules/service/application/usecase/` (UploadServiceImageUseCase, ReorderServiceImagesUseCase, etc.)
  - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceImageJpaEntity.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceImageRepository.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceRepository.java`
  - `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/UploadServiceImageUseCaseTest.java`
  - `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/ReorderServiceImagesUseCaseTest.java`
- **Key findings**:
  - `FileStoragePort` has two methods: `uploadFile(byte[], String, String)` returning String URL, and `deleteFile(String)`.
  - `UploadServiceImageUseCase` requires specific execution order verified by tests: Service existence & ownership check -> File validation (empty, MIME: JPEG/PNG/WEBP) -> Max images check (10 limit) -> Sort order determination (max + 1, or 1 if none) -> Cloudinary upload (`services/{serviceId}/images`) -> Persistence.
  - `DeleteServiceImageUseCase` is not yet created and must be implemented for M2. It needs to check ownership, find image via `findByIdAndServiceId`, delete remote file via `fileStoragePort.deleteFile`, and delete DB record.
  - `ReorderServiceImagesUseCase` requires strict validation: Service existence & ownership -> Count mismatch check (`IllegalArgumentException`) -> Duplicate IDs check (`IllegalArgumentException`) -> IDOR check against other services (`UnauthorizedServiceAccessException`) -> Update sortOrder (1-based index) -> `saveAll`.
- **Unexplored areas**: None for M2 application layer.

## Key Decisions Made
- All specifications, order of validation, and exact exceptions mapped to existing unit tests. Ready to generate handoff.md.

## Artifact Index
- handoff.md — Final handoff report
- progress.md — Liveness heartbeat and progress log
- DISPATCH.md — Initial dispatch log
