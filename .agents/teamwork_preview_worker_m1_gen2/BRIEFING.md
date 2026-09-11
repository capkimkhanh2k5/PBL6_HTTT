# BRIEFING — 2026-09-10T10:54:30+07:00

## Mission
Implement Milestone 1 (Domain Exceptions, Storage Port/Adapter, Repositories, Mappers, and Use Cases) for the Vendor Profile module.

## 🔒 My Identity
- Archetype: Worker 1 (Gen 2)
- Roles: implementer
- Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_worker_m1_gen2
- Original parent: f6c3093b-5d45-4a16-b1ff-1fb079da1cc9
- Milestone: Milestone 1 - Core Domain, Storage & Use Cases

## 🔒 Key Constraints
- Strictly adhere to Clean Architecture rules (`backend/docs/Clean_Architecture_Rules.md`).
- Only create/modify files within Milestone 1 ownership scope.
- Do not touch presentation layer (M2) or test classes (M3).
- Must verify with `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw compile test-compile` in `backend/`.
- No mock/hardcoded fake solutions. Real, genuine business logic.

## Current Parent
- Conversation ID: f6c3093b-5d45-4a16-b1ff-1fb079da1cc9
- Updated: 2026-09-10T10:54:30+07:00

## Task Summary
- **What to build**: Domain exceptions, storage port and adapter, commands, mappers, repository query methods, and 5 application use cases.
- **Success criteria**: Clean compilation with 0 errors via `./mvnw compile test-compile`.
- **Interface contracts**: PROJECT.md § Interface Contracts
- **Code layout**: PROJECT.md § Code Layout

## Key Decisions Made
- Used records for `RegisterVendorProfileCommand` and `UpdateVendorProfileCommand` with standard JavaBean getters.
- Use `@Service` on use cases with constructor injection (`@RequiredArgsConstructor`) and `@Transactional`.
- Added `findByUserId` and `existsByUserId` to `JpaVendorRepository`.
- Added `findByVendorId` and `findAllByVendorId` to `JpaVendorDocumentRepository`.
- Implemented `CloudinaryDocumentStorageAdapter` as a `@Component` implementing `DocumentStoragePort`.
- Implemented `VendorMapper` and `VendorDocumentMapper` under `infrastructure/mapper`.

## Artifact Index
- `.agents/teamwork_preview_worker_m1_gen2/BRIEFING.md` — Agent working memory
- `.agents/teamwork_preview_worker_m1_gen2/progress.md` — Progress tracker and heartbeat
- `.agents/teamwork_preview_worker_m1_gen2/handoff.md` — Final handoff report

## Change Tracker
- **Files modified**:
  - `backend/src/main/java/com/danasea/backend/modules/vendor/domain/exception/VendorAlreadyExistsException.java`: Domain exception (409)
  - `backend/src/main/java/com/danasea/backend/modules/vendor/domain/exception/VendorNotFoundException.java`: Domain exception (404)
  - `backend/src/main/java/com/danasea/backend/modules/vendor/domain/exception/UserLockedException.java`: Domain exception (403)
  - `backend/src/main/java/com/danasea/backend/modules/vendor/domain/exception/InvalidDocTypeException.java`: Domain exception (400)
  - `backend/src/main/java/com/danasea/backend/modules/vendor/application/port/DocumentStoragePort.java`: Storage port interface
  - `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/storage/CloudinaryDocumentStorageAdapter.java`: Storage adapter implementation
  - `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/repositories/JpaVendorRepository.java`: Added findByUserId & existsByUserId
  - `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/repositories/JpaVendorDocumentRepository.java`: Added findByVendorId & findAllByVendorId
  - `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/mapper/VendorMapper.java`: Mapper between VendorJpaEntity and Vendor domain model
  - `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/mapper/VendorDocumentMapper.java`: Mapper between VendorDocumentJpaEntity and VendorDocument domain model
  - `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/RegisterVendorProfileCommand.java`: Command record for registration
  - `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/RegisterVendorProfileUseCase.java`: Registration use case with user verification and role update
  - `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/GetVendorProfileUseCase.java`: Fetch vendor profile by userId
  - `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/UpdateVendorProfileCommand.java`: Command record for profile update
  - `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/UpdateVendorProfileUseCase.java`: Update use case with mass-assignment protection
  - `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/UploadVendorDocumentUseCase.java`: Document upload use case with validation
  - `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/GetVendorDocumentsUseCase.java`: Fetch documents for vendor

## Quality Status
- **Build/test result**: PASS (compiles 209 main source files, 10 test files with 0 errors)
- **Lint status**: Clean
- **Tests added/modified**: Milestone 1 complete, tests to be written in M3
