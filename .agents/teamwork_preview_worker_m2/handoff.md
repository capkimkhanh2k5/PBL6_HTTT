# Handoff Report: Milestone 2 (Presentation Layer & Security Integration)

**Module**: Vendor Profile Module  
**Agent**: Worker 2 (`teamwork_preview_worker_m2`)  
**Date**: 2026-09-10T11:00:00+07:00  
**Status**: COMPLETED  

---

## 1. Observation

### 1.1 Files Created
The following 6 files were created in the Presentation layer:

1. **DTOs** (`com.danasea.backend.modules.vendor.presentation.dto`):
   - `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/dto/RegisterVendorProfileRequest.java`:
     - Fields validated with `@NotBlank`: `businessName`, `taxCode`, `address`, `bankAccountNumber`, `bankName`, `bankAccountHolder`.
     - Supports both camelCase and snake_case via `@JsonAlias` (e.g. `business_name`, `tax_code`).
     - Includes `toCommand()` mapping directly to `RegisterVendorProfileCommand`.
   - `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/dto/UpdateVendorProfileRequest.java`:
     - Fields: `businessName`, `taxCode`, `address`, `bankAccountNumber`, `bankName`, `bankAccountHolder`.
     - Strictly excludes `verification_status`, `rating_avg`, `rating_count`, and `badge_tier` to prevent mass assignment vulnerabilities.
     - Includes `toCommand()` mapping to `UpdateVendorProfileCommand`.
   - `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/dto/VendorProfileResponse.java`:
     - Comprehensive vendor response record exposing: `id`, `userId`, `businessName`, `taxCode`, `address`, `bankAccountNumber`, `bankName`, `bankAccountHolder`, `verificationStatus`, `badgeTier`, `ratingAvg`, `ratingCount`, `createdAt`, `updatedAt`.
     - Provides static factory method `fromDomain(Vendor vendor)`.
   - `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/dto/VendorDocumentResponse.java`:
     - Document response record exposing: `id`, `vendorId`, `docType`, `fileUrl`, `status`, `reviewedBy`, `reviewedAt`, `createdAt`, `updatedAt`.
     - Provides static factory method `fromDomain(VendorDocument doc)`.

2. **Exception Handling Advice** (`com.danasea.backend.modules.vendor.presentation.advice`):
   - `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/advice/VendorExceptionHandler.java`:
     - Annotated with `@RestControllerAdvice(basePackages = "com.danasea.backend.modules.vendor")`.
     - Handles `VendorAlreadyExistsException` -> HTTP 409 Conflict (`code: "VENDOR_ALREADY_EXISTS"`).
     - Handles `VendorNotFoundException` -> HTTP 404 Not Found (`code: "VENDOR_NOT_FOUND"`).
     - Handles `UserLockedException` -> HTTP 403 Forbidden (`code: "USER_LOCKED"`).
     - Handles `InvalidDocTypeException` -> HTTP 400 Bad Request (`code: "INVALID_DOC_TYPE"`).
     - Handles `MethodArgumentNotValidException` -> HTTP 400 Bad Request (`code: "INVALID_INPUT"`).
     - Handles `IllegalArgumentException` -> HTTP 400 Bad Request (`code: "INVALID_INPUT"`).
     - Returns unified `com.danasea.backend.shared.presentation.ErrorResponse`.

3. **REST Controller** (`com.danasea.backend.modules.vendor.presentation.controller`):
   - `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/controller/VendorProfileController.java`:
     - Base mapping: `@RequestMapping("/api/vendor")`.
     - `POST /api/vendor/profile`: `@PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR')")` -> registers profile, returns HTTP 201 Created.
     - `GET /api/vendor/profile`: `@PreAuthorize("hasAnyRole('VENDOR', 'CUSTOMER')")` -> returns 200 with profile for VENDOR, throws 404 for CUSTOMER without profile, rejects ADMIN with 403 Forbidden.
     - `PATCH /api/vendor/profile`: `@PreAuthorize("hasRole('VENDOR')")` -> strictly derives userId from authenticated principal, prevents cross-vendor tampering, returns 200 with updated profile.
     - `POST /api/vendor/documents`: `@PreAuthorize("hasRole('VENDOR')")` -> takes `MultipartFile file` and `@RequestParam("doc_type")` (supporting camelCase fallback `docType`), returns HTTP 201 Created.
     - `GET /api/vendor/documents`: `@PreAuthorize("hasRole('VENDOR')")` -> returns HTTP 200 with list of documents.
     - Identity extraction: `resolveUserId(Principal principal)` securely queries `AccountInternalApi.findUserByEmail(principal.getName())` and falls back to UUID lookup if principal is formatted as UUID. Throws `AccessDeniedException` if unauthenticated and `VendorNotFoundException` if user record is missing.

### 1.2 Verification Command and Output
Executed command:
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw compile test-compile
```
Verbatim result:
```
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------< com.danasea:backend >-------------------------
[INFO] Building  0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.5.0:resources (default-resources) @ backend ---
[INFO] Copying 2 resources from src/main/resources to target/classes
[INFO] Copying 0 resource from src/main/resources to target/classes
[INFO] 
[INFO] --- compiler:3.13.0:compile (default-compile) @ backend ---
[INFO] Recompiling the module because of changed source code.
[INFO] Compiling 215 source files with javac [debug parameters release 21] to target/classes
[WARNING] /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/backend/src/main/java/com/danasea/backend/config/RabbitMQConfig.java:[89,20] org.springframework.amqp.support.converter.Jackson2JsonMessageConverter in org.springframework.amqp.support.converter has been deprecated and marked for removal
[INFO] /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/backend/src/main/java/com/danasea/backend/security/authentication/infrastructure/config/RateLimitConfig.java: Some input files use or override a deprecated API.
[INFO] /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/backend/src/main/java/com/danasea/backend/security/authentication/infrastructure/config/RateLimitConfig.java: Recompile with -Xlint:deprecation for details.
[INFO] 
[INFO] --- resources:3.5.0:testResources (default-testResources) @ backend ---
[INFO] skip non existing resourceDirectory /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/backend/src/test/resources
[INFO] 
[INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ backend ---
[INFO] Recompiling the module because of changed dependency.
[INFO] Compiling 10 source files with javac [debug parameters release 21] to target/test-classes
[INFO] /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/backend/src/test/java/com/danasea/backend/security/authentication/presentation/filter/RateLimitFilterIntegrationTest.java: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/backend/src/test/java/com/danasea/backend/security/authentication/presentation/filter/RateLimitFilterIntegrationTest.java uses or overrides a deprecated API.
[INFO] /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/backend/src/test/java/com/danasea/backend/security/authentication/presentation/filter/RateLimitFilterIntegrationTest.java: Recompile with -Xlint:deprecation for details.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  6.040 s
[INFO] Finished at: 2026-09-10T10:59:01+07:00
[INFO] ------------------------------------------------------------------------
```

---

## 2. Logic Chain

1. **Security & Role-Based Access Control**:
   - In `VendorProfileController`, method security (`@PreAuthorize`) is utilized on all endpoints.
   - For `POST /api/vendor/profile`, `@PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR')")` permits customers to register.
   - For `GET /api/vendor/profile`, `@PreAuthorize("hasAnyRole('VENDOR', 'CUSTOMER')")` allows a customer or vendor through the security filter. If an ADMIN attempts access, Spring Security denies access with HTTP 403 Forbidden. If a CUSTOMER without a profile calls this endpoint, the controller resolves their identity and invokes `GetVendorProfileUseCase`, which throws `VendorNotFoundException`, properly returning HTTP 404 Not Found as required by R3.
   - For `PATCH /api/vendor/profile`, only `ROLE_VENDOR` is permitted. The vendor entity is queried exclusively via the authenticated user's ID derived from `Principal principal`, completely avoiding path parameter tampering or cross-vendor manipulation.
   - Document upload and retrieval routes require `ROLE_VENDOR`.

2. **Protection Against Mass-Assignment**:
   - `UpdateVendorProfileRequest` only exposes mutable vendor fields: `businessName`, `taxCode`, `address`, `bankAccountNumber`, `bankName`, and `bankAccountHolder`.
   - Fields such as `verification_status`, `rating_avg`, `rating_count`, and `badge_tier` are strictly excluded from the request structure, preventing clients from modifying sensitive business status.

3. **Exception Translation**:
   - `VendorExceptionHandler` targets `com.danasea.backend.modules.vendor` and converts all domain and validation exceptions into the application's unified `ErrorResponse(code, message)` schema with accurate HTTP status codes:
     - `VendorAlreadyExistsException` -> HTTP 409 Conflict (`code: "VENDOR_ALREADY_EXISTS"`)
     - `VendorNotFoundException` -> HTTP 404 Not Found (`code: "VENDOR_NOT_FOUND"`)
     - `UserLockedException` -> HTTP 403 Forbidden (`code: "USER_LOCKED"`)
     - `InvalidDocTypeException` -> HTTP 400 Bad Request (`code: "INVALID_DOC_TYPE"`)
     - `MethodArgumentNotValidException` -> HTTP 400 Bad Request (`code: "INVALID_INPUT"`)

---

## 3. Caveats

- No caveats. All DTOs, controller endpoints, exception handlers, and security contracts compile cleanly with zero errors on Java 21.

---

## 4. Conclusion

Milestone 2 is 100% complete. All presentation DTOs, `VendorProfileController`, and `VendorExceptionHandler` are implemented according to Clean Architecture and Spring Security specifications. The backend compiles with 0 errors, ready for Milestone 3 (Comprehensive Unit & Controller Test Suite).

---

## 5. Verification Method

To independently verify the implementation:

1. **Compilation Check**:
   ```bash
   cd /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/backend
   JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw compile test-compile
   ```
   *Expected outcome*: `BUILD SUCCESS`, 215 source files compiled to target/classes with 0 errors.

2. **Inspect Presentation Components**:
   - DTOs: `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/dto/`
   - Controller: `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/controller/VendorProfileController.java`
   - Advice: `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/advice/VendorExceptionHandler.java`
