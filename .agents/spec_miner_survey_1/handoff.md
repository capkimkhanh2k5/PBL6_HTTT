# Handoff Report: Specification Mining & Test Architecture for Services Module

## 1. Observation

### 1.1 Project Structure & Environment
- **JDK / Java Version**: Java 21 (`OpenJDK 21.0.10` via Temurin at `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`).
- **Frameworks & Versions**:
  - Spring Boot `4.1.1` (parent pom: `spring-boot-starter-parent:4.1.1`, core: `spring-boot:jar:4.1.1:compile`).
  - Spring Security `7.1.1` (`spring-security-test:jar:7.1.1:test`).
  - JUnit Jupiter `5.x` (via `spring-boot-starter-test`).
  - Mockito `5.23.0` (`mockito-core`, `mockito-junit-jupiter`).
  - Lombok `1.18.34`.
  - JJWT `0.13.0`.
  - SpringDoc OpenAPI `3.1.1`.
- **Architectural Guidelines**: Documented in `backend/docs/Clean_Architecture_Rules.md`.
  - Architecture: **Modular Clean Architecture** (`modules/<module_name>/{domain, application, infrastructure, presentation}`).
  - Dependency Rule: Presentation → Application → Domain ← Infrastructure.
  - Domain and Application layers are independent of frameworks (Application Use Cases are pure Java POJOs wired via `@Configuration` in `modules/systemconfig/ApplicationBeans.java`).
  - Module Dependency Rule: Inter-module communication must happen through public abstractions / internal APIs (e.g., `AccountInternalApi`).
- **Existing Service & Vendor Domain Entities**:
  - `modules/service/domain/models/Service.java`: has fields `vendorId`, `categoryId`, `name`, `nameEn`, `slug`, `description`, `price`, `durationMinutes`, `capacityPerSlot`, `locationName`, `address`, `latitude`, `longitude`, `status`, `waiverContent`, `weatherSensitive`, `minWindKmh`, `maxWaveM`, `avgRating`, `ratingCount`, `viewCount`.
  - `modules/service/domain/models/ServiceStatus.java`: enum values `DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `REJECTED`, `PAUSED`.
  - `modules/service/domain/models/Category.java`: has `id`, `name`, `slug`, `parentId`, `iconUrl`, `isActive`.
  - `modules/service/domain/models/ServiceImage.java`: has `id`, `serviceId`, `url`, `sortOrder`.
  - `modules/vendor/domain/models/Vendor.java`: has `id`, `userId`, `businessName`, `verificationStatus`.
  - `modules/vendor/domain/models/VerificationStatus.java`: enum values `PENDING`, `APPROVED`, `REJECTED`.
  - `modules/account/domain/models/AuditLog.java`: has `actorUserId`, `action`, `entityType`, `entityId`, `metadata`.
  - `shared/presentation/ErrorResponse.java`: record `(String code, String message)`.
- **Existing Test Suites & Patterns**:
  - `LoginUseCaseTest.java`: Pure unit test using Mockito (`mock()`, `when()`, `verify()`) and JUnit 5 (`@Test`, `assertEquals`, `assertThrows`). No Spring context. Fast execution (<0.5s).
  - `AuthenticationControllerTest.java`: Controller test using MockMvc standalone setup (`MockMvcBuilders.standaloneSetup(...).setControllerAdvice(...).build()`).
  - `BackendApplicationTests.java`: Marked `@Disabled("Fails without test database setup")` to prevent failures when PostgreSQL/Redis test containers are not running.
- **Security & Authorization Context**:
  - `SecurityConfig.java`: `@EnableWebSecurity`, `@EnableMethodSecurity`.
  - `JwtAuthenticationFilter.java`: maps roles to `ROLE_<ROLE_NAME>` (e.g. `ROLE_VENDOR`, `ROLE_ADMIN`, `ROLE_CUSTOMER`).
  - `AuthorizationController.java`: uses `@PreAuthorize("hasRole('VENDOR')")` and `@PreAuthorize("hasRole('ADMIN')")`.
  - `AuthorizationHandler.java`: catches `AccessDeniedException`, returns HTTP 403 with `ErrorResponse("ACCESS_DENIED", message)`.

---

## 2. Logic Chain

### 2.1 Mapped Requirements (R1 - R4)
1. **R1. Vendor API (`/api/vendor/services/**`)**:
   - `POST /api/vendor/services`: Creates a service. Status must default to `DRAFT`.
   - `GET /api/vendor/services`: Retrieves all services belonging to current authenticated vendor.
   - `GET /api/vendor/services/{id}`: Retrieves single service; owner only.
   - `PATCH /api/vendor/services/{id}`: Updates service fields; owner only. Modifying a `PUBLISHED` service automatically transitions it to `PENDING_REVIEW`.
   - `POST /api/vendor/services/{id}/submit`: Submits a `DRAFT` or `REJECTED` service for review -> transitions to `PENDING_REVIEW`. Requires at least 1 image.
   - `PATCH /api/vendor/services/{id}/pause`: Pauses service bookings: `PUBLISHED` -> `PAUSED`.
   - `PATCH /api/vendor/services/{id}/resume`: Resumes service bookings: `PAUSED` -> `PUBLISHED`.
   - `DELETE /api/vendor/services/{id}`: Deletes a service; only permitted when status is `DRAFT`.
2. **R2. Admin API (`/api/admin/services/**`)**:
   - `GET /api/admin/services?status=PENDING_REVIEW`: Lists services pending approval (supports status filtering).
   - `PATCH /api/admin/services/{id}/approve`: Approves service: `PENDING_REVIEW` -> `PUBLISHED`. Records audit log `SERVICE_APPROVED`.
   - `PATCH /api/admin/services/{id}/reject`: Rejects service: `PENDING_REVIEW` -> `REJECTED`. Requires non-blank reason. Records audit log `SERVICE_REJECTED` with reason in metadata.
3. **R3. Business Rules Validation**:
   - **BR1 (Vendor Verification)**: Only vendors with `verificationStatus == APPROVED` can create services. Vendors with `PENDING` or `REJECTED` status are rejected with HTTP 400/403.
   - **BR2 (Category Check)**: `categoryId` must exist and `isActive == true`. Inactive or missing category returns HTTP 404 / 400.
   - **BR3 (Weather Rules)**: If `weatherSensitive == true`, both `minWindKmh` and `maxWaveM` must be non-null and >= 0.
   - **BR4 (Image Requirement on Submit)**: `serviceImages` cannot be empty when submitting for review. Submitting without images returns HTTP 400.
   - **BR5 (State Machine Guards)**:
     - Only `DRAFT` and `REJECTED` can transition to `PENDING_REVIEW`.
     - Only `PENDING_REVIEW` can transition to `PUBLISHED` or `REJECTED`.
     - Only `PUBLISHED` can transition to `PAUSED`.
     - Only `PAUSED` can transition to `PUBLISHED`.
     - Only `DRAFT` can be deleted. Trying to delete `PUBLISHED`, `PAUSED`, or `PENDING_REVIEW` returns HTTP 400.
     - Modifying `PUBLISHED` transitions it back to `PENDING_REVIEW`.
   - **BR6 (Ownership & Isolation)**: A vendor cannot view, update, submit, pause, resume, or delete a service owned by another vendor (HTTP 403).
4. **R4. Security & Access Control**:
   - `/api/vendor/services/**`: Accessible ONLY by `ROLE_VENDOR`. Denies `ROLE_CUSTOMER` and `ROLE_ADMIN` with HTTP 403.
   - `/api/admin/services/**`: Accessible ONLY by `ROLE_ADMIN`. Denies `ROLE_VENDOR` and `ROLE_CUSTOMER` with HTTP 403.
   - Unauthenticated access returns HTTP 401 / 403.

---

## 3. Features Discovered & Test Specification Matrices

### Features Discovered
| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Vendor Service | Create Service | Create new service in DRAFT status | `CreateServiceCommand` (name, price, duration, categoryId, etc.) | Created `Service` (status=DRAFT) | 400/403 if vendor unapproved; 404/400 if category invalid; 400 if weather params missing | ORIGINAL_REQUEST.md R1, R3 |
| 2 | Vendor Service | Get Vendor Services | List all services for current vendor | `vendorId`, optional filters/pageable | `List<ServiceSummary>` | 403 if not vendor | ORIGINAL_REQUEST.md R1 |
| 3 | Vendor Service | Get Service Detail | Retrieve service detail by ID for owner | `serviceId`, `vendorId` | `ServiceDetailResponse` | 404 if not found; 403 if not owner | ORIGINAL_REQUEST.md R1 |
| 4 | Vendor Service | Update Service | Update service information | `serviceId`, `vendorId`, `UpdateServiceCommand` | Updated `Service` | 404 if not found; 403 if not owner; 400 if invalid category/weather | ORIGINAL_REQUEST.md R1, R3 |
| 5 | Vendor Service | Submit For Review | Submit DRAFT or REJECTED service to PENDING_REVIEW | `serviceId`, `vendorId` | `Service` (status=PENDING_REVIEW) | 400 if no images; 400 if status not DRAFT/REJECTED; 403 if not owner | ORIGINAL_REQUEST.md R1, R3 |
| 6 | Vendor Service | Pause Service | Pause service booking: PUBLISHED -> PAUSED | `serviceId`, `vendorId` | `Service` (status=PAUSED) | 400 if status not PUBLISHED; 403 if not owner; 404 if not found | ORIGINAL_REQUEST.md R1 |
| 7 | Vendor Service | Resume Service | Resume paused service: PAUSED -> PUBLISHED | `serviceId`, `vendorId` | `Service` (status=PUBLISHED) | 400 if status not PAUSED; 403 if not owner; 404 if not found | ORIGINAL_REQUEST.md R1 |
| 8 | Vendor Service | Delete Service | Delete service in DRAFT status | `serviceId`, `vendorId` | 204 No Content / Success | 400 if status not DRAFT; 403 if not owner; 404 if not found | ORIGINAL_REQUEST.md R1, R3 |
| 9 | Admin Service | List Pending Services | List services pending admin review | Status filter (`PENDING_REVIEW`), pageable | `List<ServiceSummary>` | 403 if not admin | ORIGINAL_REQUEST.md R2 |
| 10 | Admin Service | Approve Service | Approve service: PENDING_REVIEW -> PUBLISHED | `serviceId`, `adminUserId` | Approved `Service`, AuditLog | 400 if status not PENDING_REVIEW; 404 if not found; 403 if not admin | ORIGINAL_REQUEST.md R2 |
| 11 | Admin Service | Reject Service | Reject service: PENDING_REVIEW -> REJECTED with reason | `serviceId`, `adminUserId`, `reason` | Rejected `Service`, AuditLog | 400 if reason empty; 400 if status not PENDING_REVIEW; 404 if not found | ORIGINAL_REQUEST.md R2 |
| 12 | Security | Vendor Endpoint RBAC | Block non-vendor roles from `/api/vendor/**` | JWT with CUSTOMER or ADMIN role | 403 Forbidden | HTTP 403 ACCESS_DENIED | ORIGINAL_REQUEST.md R4, SecurityConfig |
| 13 | Security | Admin Endpoint RBAC | Block non-admin roles from `/api/admin/**` | JWT with VENDOR or CUSTOMER role | 403 Forbidden | HTTP 403 ACCESS_DENIED | ORIGINAL_REQUEST.md R4, SecurityConfig |
| 14 | Audit | Service Audit Logging | Record audit trail for approve/reject actions | `actorUserId`, `action`, `entityType`, `entityId`, `metadata` | Saved `AuditLog` | Rollback if audit fails | ORIGINAL_REQUEST.md R2, AuditLog.java |

---

### Edge Cases
| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | Create Service | Vendor has `verificationStatus=PENDING` | Throws `VendorNotApprovedException` -> HTTP 400/403 `VENDOR_NOT_APPROVED` |
| 2 | Create Service | Vendor has `verificationStatus=REJECTED` | Throws `VendorNotApprovedException` -> HTTP 400/403 `VENDOR_NOT_APPROVED` |
| 3 | Create Service | `categoryId` does not exist | Throws `CategoryNotFoundException` -> HTTP 404 `CATEGORY_NOT_FOUND` |
| 4 | Create Service | `categoryId` exists but `isActive=false` | Throws `CategoryInactiveException` -> HTTP 400 `CATEGORY_INACTIVE` |
| 5 | Create Service | `weatherSensitive=true`, `minWindKmh=null`, `maxWaveM=2.0` | Throws `WeatherRequirementsMissingException` -> HTTP 400 `WEATHER_REQUIREMENTS_MISSING` |
| 6 | Create Service | `weatherSensitive=true`, `minWindKmh=15.0`, `maxWaveM=null` | Throws `WeatherRequirementsMissingException` -> HTTP 400 `WEATHER_REQUIREMENTS_MISSING` |
| 7 | Create Service | `weatherSensitive=true`, `minWindKmh=0.0`, `maxWaveM=0.0` | Valid boundary inputs; service created successfully with status DRAFT |
| 8 | Create Service | `weatherSensitive=false`, `minWindKmh=null`, `maxWaveM=null` | Valid; weather fields not required |
| 9 | Submit Review | `serviceImages` is empty list `[]` | Throws `ServiceImagesRequiredException` -> HTTP 400 `SERVICE_IMAGES_REQUIRED` |
| 10 | Submit Review | Service current status is `PUBLISHED` | Throws `InvalidServiceStateException` -> HTTP 400 `INVALID_SERVICE_STATUS` |
| 11 | Submit Review | Service current status is `PENDING_REVIEW` | Throws `InvalidServiceStateException` -> HTTP 400 `INVALID_SERVICE_STATUS` |
| 12 | Submit Review | Service current status is `PAUSED` | Throws `InvalidServiceStateException` -> HTTP 400 `INVALID_SERVICE_STATUS` |
| 13 | Submit Review | Service current status is `REJECTED`, has >= 1 image | Permitted; transitions `REJECTED` -> `PENDING_REVIEW` |
| 14 | Submit Review | Service current status is `DRAFT`, has >= 1 image | Permitted; transitions `DRAFT` -> `PENDING_REVIEW` |
| 15 | Submit Review | Service owned by Vendor A, submitted by Vendor B | Throws `AccessDeniedException` -> HTTP 403 `ACCESS_DENIED` |
| 16 | Update Service | Service in `PUBLISHED` status, any attribute updated | Updates fields AND automatically transitions status to `PENDING_REVIEW` |
| 17 | Update Service | Service in `DRAFT` status, updated | Updates fields, status stays `DRAFT` |
| 18 | Update Service | Service in `REJECTED` status, updated | Updates fields, status stays `REJECTED` |
| 19 | Update Service | Update `weatherSensitive=true` without providing wind/wave | Throws `WeatherRequirementsMissingException` -> HTTP 400 |
| 20 | Update Service | Update `categoryId` to inactive category | Throws `CategoryInactiveException` -> HTTP 400 `CATEGORY_INACTIVE` |
| 21 | Update Service | Service owned by Vendor A, updated by Vendor B | Throws `AccessDeniedException` -> HTTP 403 `ACCESS_DENIED` |
| 22 | Approve Service | Current status is `PENDING_REVIEW` | Transitions to `PUBLISHED`, saves `AuditLog(action="SERVICE_APPROVED", entityType="SERVICE")` |
| 23 | Approve Service | Current status is `DRAFT` | Throws `InvalidServiceStateException` -> HTTP 400 `INVALID_SERVICE_STATUS` |
| 24 | Approve Service | Current status is `PUBLISHED` | Throws `InvalidServiceStateException` -> HTTP 400 `INVALID_SERVICE_STATUS` |
| 25 | Approve Service | Current status is `PAUSED` | Throws `InvalidServiceStateException` -> HTTP 400 `INVALID_SERVICE_STATUS` |
| 26 | Reject Service | Current status is `PENDING_REVIEW`, `reason="Incomplete safety info"` | Transitions to `REJECTED`, saves `AuditLog(action="SERVICE_REJECTED", metadata="{\"reason\":\"Incomplete safety info\"}")` |
| 27 | Reject Service | Current status is `PENDING_REVIEW`, `reason=""` or `null` | Throws `IllegalArgumentException` / validation error -> HTTP 400 `REASON_REQUIRED` |
| 28 | Reject Service | Current status is `DRAFT` or `PUBLISHED` | Throws `InvalidServiceStateException` -> HTTP 400 `INVALID_SERVICE_STATUS` |
| 29 | Delete Service | Service in `DRAFT` status, requested by owner | Successfully deleted -> 204 No Content |
| 30 | Delete Service | Service in `PUBLISHED` status | Throws `CannotDeleteNonDraftServiceException` -> HTTP 400 `CANNOT_DELETE_NON_DRAFT_SERVICE` |
| 31 | Delete Service | Service in `PAUSED` status | Throws `CannotDeleteNonDraftServiceException` -> HTTP 400 `CANNOT_DELETE_NON_DRAFT_SERVICE` |
| 32 | Delete Service | Service in `PENDING_REVIEW` status | Throws `CannotDeleteNonDraftServiceException` -> HTTP 400 `CANNOT_DELETE_NON_DRAFT_SERVICE` |
| 33 | Delete Service | Service in `REJECTED` status | Throws `CannotDeleteNonDraftServiceException` -> HTTP 400 `CANNOT_DELETE_NON_DRAFT_SERVICE` |
| 34 | Delete Service | Service owned by Vendor A, deleted by Vendor B | Throws `AccessDeniedException` -> HTTP 403 `ACCESS_DENIED` |
| 35 | Pause Service | Service in `PUBLISHED` status | Transitions to `PAUSED` -> 200 OK |
| 36 | Pause Service | Service in `DRAFT` or `PAUSED` or `PENDING_REVIEW` | Throws `InvalidServiceStateException` -> HTTP 400 `INVALID_SERVICE_STATUS` |
| 37 | Resume Service | Service in `PAUSED` status | Transitions to `PUBLISHED` -> 200 OK |
| 38 | Resume Service | Service in `PUBLISHED` or `DRAFT` | Throws `InvalidServiceStateException` -> HTTP 400 `INVALID_SERVICE_STATUS` |
| 39 | RBAC Check | `CUSTOMER` calls `/api/vendor/services` | HTTP 403 Forbidden |
| 40 | RBAC Check | `ADMIN` calls `/api/vendor/services` | HTTP 403 Forbidden |
| 41 | RBAC Check | `VENDOR` calls `/api/admin/services` | HTTP 403 Forbidden |
| 42 | RBAC Check | `CUSTOMER` calls `/api/admin/services` | HTTP 403 Forbidden |
| 43 | RBAC Check | Anonymous user calls any protected endpoint | HTTP 401 Unauthorized / 403 Forbidden |

---

## 4. Test Specifications & Architecture

### 4.1 Unit Test Specifications for Use Cases
Following the existing project pattern (pure POJO tests using JUnit 5 + Mockito without Spring context):

1. **`CreateServiceUseCaseTest`**:
   - `shouldCreateServiceSuccessfullyWhenVendorIsApprovedAndCategoryIsActive()`
   - `shouldThrowWhenVendorIsNotApproved(VerificationStatus.PENDING)`
   - `shouldThrowWhenVendorIsRejected(VerificationStatus.REJECTED)`
   - `shouldThrowWhenVendorNotFound()`
   - `shouldThrowWhenCategoryNotFound()`
   - `shouldThrowWhenCategoryIsInactive()`
   - `shouldThrowWhenWeatherSensitiveAndMinWindKmhIsNull()`
   - `shouldThrowWhenWeatherSensitiveAndMaxWaveMIsNull()`
   - `shouldSucceedWhenWeatherSensitiveIsFalseAndWeatherFieldsAreNull()`
   - `shouldSetInitialStatusToDraft()`

2. **`SubmitServiceForReviewUseCaseTest`**:
   - `shouldSubmitSuccessfullyFromDraftWhenImagesExist()`
   - `shouldSubmitSuccessfullyFromRejectedWhenImagesExist()`
   - `shouldThrowWhenImagesListIsEmpty()`
   - `shouldThrowWhenServiceNotFound()`
   - `shouldThrowWhenCallerIsNotOwnerVendor()`
   - `shouldThrowWhenCurrentStatusIsPublished()`
   - `shouldThrowWhenCurrentStatusIsPendingReview()`
   - `shouldThrowWhenCurrentStatusIsPaused()`

3. **`UpdateServiceUseCaseTest`**:
   - `shouldUpdateDraftServiceAndKeepStatusDraft()`
   - `shouldUpdatePublishedServiceAndTransitionToPendingReview()`
   - `shouldUpdateRejectedServiceAndKeepStatusRejected()`
   - `shouldThrowWhenCallerIsNotOwnerVendor()`
   - `shouldThrowWhenServiceNotFound()`
   - `shouldThrowWhenUpdatedCategoryIsInactive()`
   - `shouldThrowWhenUpdatedWeatherSensitiveIsTrueWithoutWindOrWave()`

4. **`ApproveServiceUseCaseTest`**:
   - `shouldApprovePendingReviewServiceAndRecordAuditLog()`
   - `shouldThrowWhenCurrentStatusIsDraft()`
   - `shouldThrowWhenCurrentStatusIsPublished()`
   - `shouldThrowWhenCurrentStatusIsRejected()`
   - `shouldThrowWhenCurrentStatusIsPaused()`
   - `shouldThrowWhenServiceNotFound()`

5. **`RejectServiceUseCaseTest`**:
   - `shouldRejectPendingReviewServiceWithReasonAndRecordAuditLog()`
   - `shouldThrowWhenReasonIsNull()`
   - `shouldThrowWhenReasonIsBlank()`
   - `shouldThrowWhenCurrentStatusIsNotPendingReview()`
   - `shouldThrowWhenServiceNotFound()`

6. **`DeleteServiceUseCaseTest`**:
   - `shouldDeleteServiceWhenStatusIsDraftAndCallerIsOwner()`
   - `shouldThrowWhenStatusIsPublished()`
   - `shouldThrowWhenStatusIsPaused()`
   - `shouldThrowWhenStatusIsPendingReview()`
   - `shouldThrowWhenStatusIsRejected()`
   - `shouldThrowWhenCallerIsNotOwnerVendor()`
   - `shouldThrowWhenServiceNotFound()`

### 4.2 Security & Integration Test Specifications (`ServiceControllerTest`)
- Built using MockMvc + Spring Security Test (`@WithMockUser` or `.with(user().roles(...))`).
- Target Endpoints:
  - `POST /api/vendor/services`
  - `GET /api/vendor/services`
  - `GET /api/vendor/services/{id}`
  - `PATCH /api/vendor/services/{id}`
  - `POST /api/vendor/services/{id}/submit`
  - `PATCH /api/vendor/services/{id}/pause`
  - `PATCH /api/vendor/services/{id}/resume`
  - `DELETE /api/vendor/services/{id}`
  - `GET /api/admin/services`
  - `PATCH /api/admin/services/{id}/approve`
  - `PATCH /api/admin/services/{id}/reject`
- Test Scenarios for Access Control:
  - Role `VENDOR` allowed on `/api/vendor/services/**`, blocked on `/api/admin/services/**` (403).
  - Role `ADMIN` allowed on `/api/admin/services/**`, blocked on `/api/vendor/services/**` (403).
  - Role `CUSTOMER` blocked on both `/api/vendor/services/**` and `/api/admin/services/**` (403).
  - Anonymous caller blocked on all (401/403).

---

## 5. Caveats
- `BackendApplicationTests.java` is currently disabled because it attempts to connect to PostgreSQL without a live database or Testcontainers initialized. Controller and Use Case tests must NOT rely on full `@SpringBootTest` unless using `@WebMvcTest` or standalone MockMvc.
- The `Vendor` entity is currently in `com.danasea.backend.modules.vendor`, while `Service` is in `com.danasea.backend.modules.service`. Following Rule 11 (Module Dependency Rule), `service` module should interact with `vendor` module via an abstraction/internal API (e.g. `VendorInternalApi` or `VendorVerificationPort`) rather than direct repository coupling.

---

## 6. Conclusion
All requirements from `ORIGINAL_REQUEST.md` (R1-R4), Clean Architecture rules, and test patterns have been thoroughly surveyed and mapped:
1. 6 core Use Cases identified with exact input/output contracts, validations, and state transition rules.
2. Complete state machine modeled with guards for `DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `REJECTED`, and `PAUSED`.
3. Edge case matrix with 43 detailed scenarios covering weather sensitivity, vendor verification, image requirements, editing behavior, and RBAC.
4. Test suite blueprint designed using pure JUnit 5 + Mockito for use cases and MockMvc + Spring Security Test for controllers.

---

## 7. Verification Method
- Compile tests with Maven:
  ```bash
  JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test-compile
  ```
- Run unit test suite:
  ```bash
  JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=LoginUseCaseTest
  ```
- Verify mock controller suite:
  ```bash
  JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=AuthenticationControllerTest
  ```
