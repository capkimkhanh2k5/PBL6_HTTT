# Dispatch to Worker 2: Milestone 2 (Presentation Layer & Security Integration)

You are a Worker agent implementing Milestone 2 of the Vendor Profile module.
Your working directory is: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_worker_m2
Project root is: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module
Backend directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/backend

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Read:
- /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/ORIGINAL_REQUEST.md
- /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_orchestrator_1/PROJECT.md
- Milestone 1 handoff in /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_worker_m1_gen2/handoff.md

## Scope & File Ownership
You exclusively own and will create:
1. DTOs in `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/dto/`:
   - `RegisterVendorProfileRequest.java`:
     Fields with `@NotBlank`: `businessName`, `taxCode`, `address`, `bankAccountNumber`, `bankName`, `bankAccountHolder`.
   - `UpdateVendorProfileRequest.java`:
     MUST strictly exclude `verification_status`, `rating_avg`, and `badge_tier`.
     Fields: `businessName`, `taxCode`, `address`, `bankAccountNumber`, `bankName`, `bankAccountHolder`.
   - `VendorProfileResponse.java`:
     Returns full vendor info: `id`, `userId`, `businessName`, `taxCode`, `address`, `bankAccountNumber`, `bankName`, `bankAccountHolder`, `verificationStatus`, `badgeTier`, `ratingAvg`, `ratingCount`, `createdAt`, `updatedAt`.
   - `VendorDocumentResponse.java`:
     Returns document info: `id`, `vendorId`, `docType`, `fileUrl`, `status`, `reviewedBy`, `reviewedAt`, `createdAt`, `updatedAt`.
2. REST Controller in `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/controller/`:
   - `VendorProfileController.java`:
     - Base path `/api/vendor`
     - `POST /api/vendor/profile`: CUSTOMER registers as Vendor. `@PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR')")`
     - `GET /api/vendor/profile`: Vendor views own profile. `@PreAuthorize("hasAnyRole('VENDOR', 'CUSTOMER')")` (Ensures ADMIN gets 403 Forbidden; CUSTOMER without a profile passes auth but UseCase throws `VendorNotFoundException` -> returns 404 Not Found as required by R3).
     - `PATCH /api/vendor/profile`: Vendor edits profile. `@PreAuthorize("hasRole('VENDOR')")`. Strictly uses authenticated userId from SecurityContext/Principal (NEVER path variable or request param).
     - `POST /api/vendor/documents`: Upload document. `@PreAuthorize("hasRole('VENDOR')")`. Takes `MultipartFile file` and `@RequestParam("doc_type") String docType`.
     - `GET /api/vendor/documents`: View documents. `@PreAuthorize("hasRole('VENDOR')")`.
     - Helper method to extract UUID `userId`: Controller injects `AccountInternalApi`. From `Principal principal`, gets `principal.getName()` (email), calls `accountInternalApi.findUserByEmail(...)` to get `User.getId()`. If not found, throws `VendorNotFoundException` or `AccessDeniedException`.
3. Exception Handler in `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/advice/`:
   - `VendorExceptionHandler.java`:
     - `@RestControllerAdvice(basePackages = "com.danasea.backend.modules.vendor")`
     - Handle `VendorAlreadyExistsException` -> HTTP 409 Conflict (`code: "VENDOR_ALREADY_EXISTS"`)
     - Handle `VendorNotFoundException` -> HTTP 404 Not Found (`code: "VENDOR_NOT_FOUND"`)
     - Handle `UserLockedException` -> HTTP 403 Forbidden (`code: "USER_LOCKED"`)
     - Handle `InvalidDocTypeException` -> HTTP 400 Bad Request (`code: "INVALID_DOC_TYPE"`)
     - Handle `MethodArgumentNotValidException` -> HTTP 400 Bad Request (`code: "INVALID_INPUT"`)
     - Uses `com.danasea.backend.shared.presentation.ErrorResponse`.

## Verification Requirement
You MUST run the compilation check using Java 21:
`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw compile test-compile`
Ensure the build succeeds with 0 compile errors.
Document the exact command output in your `handoff.md`.
Communicate back using `send_message` when complete.

## 2026-09-10T03:54:53Z
You are Worker 2 implementing Milestone 2 (Presentation Layer & Security Integration: DTOs, VendorProfileController, VendorExceptionHandler) for the Vendor Profile module.
Your working directory is: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_worker_m2
Read your instructions in: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_worker_m2/DISPATCH.md
Read the original request: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/ORIGINAL_REQUEST.md
Read the project document: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_orchestrator_1/PROJECT.md

Implement all Milestone 2 components cleanly, adhering to Clean Architecture and Spring Security.
MANDATORY: Verify by running `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw compile test-compile` in backend/.
Write your handoff report to: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_worker_m2/handoff.md
Communicate back via send_message when complete.
