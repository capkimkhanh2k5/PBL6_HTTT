# Dispatch to Worker 1 (Gen 2): Milestone 1 (Domain, Storage Port/Adapter, and Use Cases)

You are a Worker agent implementing Milestone 1 of the Vendor Profile module.
Your working directory is: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_worker_m1_gen2
Project root is: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module
Backend directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/backend

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Read:
- /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/ORIGINAL_REQUEST.md
- /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_orchestrator_1/PROJECT.md
- Survey reports in:
  - /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_explorer_survey_1/handoff.md
  - /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_explorer_survey_2/handoff.md
  - /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_spec_miner_survey_3/handoff.md

## Scope & File Ownership
You exclusively own and will create/modify:
1. Domain Exceptions:
   - `backend/src/main/java/com/danasea/backend/modules/vendor/domain/exception/VendorAlreadyExistsException.java`
   - `backend/src/main/java/com/danasea/backend/modules/vendor/domain/exception/VendorNotFoundException.java`
   - `backend/src/main/java/com/danasea/backend/modules/vendor/domain/exception/UserLockedException.java`
   - `backend/src/main/java/com/danasea/backend/modules/vendor/domain/exception/InvalidDocTypeException.java`
2. Storage Port and Adapter:
   - `backend/src/main/java/com/danasea/backend/modules/vendor/application/port/DocumentStoragePort.java`
   - `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/storage/CloudinaryDocumentStorageAdapter.java`
3. Commands & Application Use Cases:
   - `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/RegisterVendorProfileUseCase.java` (and RegisterVendorProfileCommand)
   - `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/GetVendorProfileUseCase.java`
   - `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/UpdateVendorProfileUseCase.java` (and UpdateVendorProfileCommand)
   - `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/UploadVendorDocumentUseCase.java`
   - `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/GetVendorDocumentsUseCase.java`
4. Update repository query methods if needed in:
   - `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/repositories/JpaVendorRepository.java` (e.g. `Optional<VendorJpaEntity> findByUserId(UUID userId);`, `boolean existsByUserId(UUID userId);`)
   - `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/repositories/JpaVendorDocumentRepository.java` (e.g. `List<VendorDocumentJpaEntity> findByVendorId(UUID vendorId);`)

## Implementation Details
1. `RegisterVendorProfileUseCase`:
   - Checks user exists via `AccountInternalApi.findUserById(userId)`.
   - If `user.getIsLocked()` is true -> throw `UserLockedException`.
   - If vendor already exists (`jpaVendorRepository.existsByUserId(userId)`) -> throw `VendorAlreadyExistsException`.
   - Create Vendor with `verificationStatus = VerificationStatus.PENDING`, `badgeTier = BadgeTier.NONE`, `ratingAvg = BigDecimal.ZERO`, `ratingCount = 0`.
   - Save Vendor.
   - Update user role: `user.setRole(Role.VENDOR)` and call `accountInternalApi.saveUser(user)`.
   - Return created Vendor.
2. `GetVendorProfileUseCase`:
   - Fetch Vendor by `userId`. If not found -> throw `VendorNotFoundException`.
3. `UpdateVendorProfileUseCase`:
   - Fetch Vendor by `userId`. If not found -> throw `VendorNotFoundException`.
   - Update only: `businessName`, `taxCode`, `address`, `bankAccountNumber`, `bankName`, `bankAccountHolder`.
   - Strictly DO NOT update `verificationStatus`, `ratingAvg`, `ratingCount`, `badgeTier`.
   - Save and return updated Vendor.
4. `UploadVendorDocumentUseCase`:
   - Parse / validate `docType`. If not valid `DocType` enum (`BUSINESS_LICENSE`, `SAFETY_CERT`) -> throw `InvalidDocTypeException`.
   - Fetch Vendor by `userId`. If not found -> throw `VendorNotFoundException`.
   - Upload file via `DocumentStoragePort.uploadDocument(file, "vendor_documents")`.
   - Create VendorDocument with `vendorId`, `docType`, `fileUrl`, `status = DocStatus.PENDING`, `reviewedBy = null`, `reviewedAt = null`.
   - Save and return VendorDocument.
5. `GetVendorDocumentsUseCase`:
   - Fetch Vendor by `userId`. If not found -> throw `VendorNotFoundException`.
   - Return all documents for `vendorId`.

## Verification Requirement
You MUST run the compilation check using Java 21:
`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw compile test-compile`
Ensure the build succeeds with 0 compile errors.
Document the exact command output in your `handoff.md`.
Communicate back using `send_message` when complete.
