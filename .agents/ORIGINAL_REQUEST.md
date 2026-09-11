# Original User Request

## 2026-09-10T03:39:58Z

Implement the Vendor Profile module for a Spring Boot application, including vendor registration, profile management, document upload APIs, and comprehensive test coverage.

Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module
Integrity mode: development

## Requirements

### R1. Implement Vendor Profile APIs
- `POST /api/vendor/profile` — CUSTOMER registers as Vendor. Creates a vendor record with `verification_status=PENDING` and changes the primary user role from CUSTOMER to VENDOR.
- `GET /api/vendor/profile` — Vendor views own profile.
- `PATCH /api/vendor/profile` — Vendor edits `business_name`, `tax_code`, `address`, `bank_account_*`. It must structurally prevent updating `verification_status`, `rating_avg`, `badge_tier` (e.g., via a strict Request DTO).
- `POST /api/vendor/documents` — Upload documents (`doc_type`: `BUSINESS_LICENSE`/`SAFETY_CERT`) to Cloudinary, creating `vendor_documents` with status `PENDING`.
- `GET /api/vendor/documents` — Vendor views submitted documents and their status.

### R2. Implement Use Case Tests
- `RegisterVendorProfileUseCaseTest`: Tests successful registration (status=PENDING, role update), duplicate registration (`VendorAlreadyExistsException`), and locked user prevention.
- `UpdateVendorProfileUseCaseTest`: Tests valid updates, mass assignment prevention (ignoring `verificationStatus`/`ratingAvg`), and cross-vendor manipulation (returning 403/404 by fetching vendor via userId from SecurityContext, not path param).
- `UploadVendorDocumentUseCaseTest`: Tests successful upload (status PENDING, `reviewed_by`/`reviewed_at` null), invalid `doc_type` not in enum (400), and upload before vendor registration (404/409).

### R3. Implement Controller Tests
- `VendorProfileControllerTest` (MockMvc): Tests 404/error for CUSTOMER without a vendor profile on GET, and 403 for ADMIN trying to access VENDOR routes.

## Acceptance Criteria

### Verification & Testing
- [ ] All specified test classes (`RegisterVendorProfileUseCaseTest`, `UpdateVendorProfileUseCaseTest`, `UploadVendorDocumentUseCaseTest`, `VendorProfileControllerTest`) are implemented and pass when running the test suite (e.g., via `mvn test` or `./gradlew test`).
- [ ] The `PATCH` endpoint uses a specific DTO that strictly excludes `verification_status`, `rating_avg`, and `badge_tier`.
- [ ] The vendor identity in the `PATCH` endpoint is derived directly from the `SecurityContext` (user ID), not from user-provided request parameters or path variables.

## 2026-09-10T04:04:43Z

RESUME EXECUTION: The previous run was interrupted by a quota limit error (429) during Milestone 2 (Worker 2). 
Please read the existing coordination files in `.agents/teamwork_preview_orchestrator_1/` (such as `PROJECT.md`, `progress.md`, `BRIEFING.md`) to understand the current state, and resume the task from where it left off to complete the remaining milestones.

Original Task:
Implement the Vendor Profile module for a Spring Boot application, including vendor registration, profile management, document upload APIs, and comprehensive test coverage.

Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module
Integrity mode: development

## Requirements

### R1. Implement Vendor Profile APIs
- `POST /api/vendor/profile` — CUSTOMER registers as Vendor. Creates a vendor record with `verification_status=PENDING` and changes the primary user role from CUSTOMER to VENDOR.
- `GET /api/vendor/profile` — Vendor views own profile.
- `PATCH /api/vendor/profile` — Vendor edits `business_name`, `tax_code`, `address`, `bank_account_*`. It must structurally prevent updating `verification_status`, `rating_avg`, `badge_tier` (e.g., via a strict Request DTO).
- `POST /api/vendor/documents` — Upload documents (`doc_type`: `BUSINESS_LICENSE`/`SAFETY_CERT`) to Cloudinary, creating `vendor_documents` with status `PENDING`.
- `GET /api/vendor/documents` — Vendor views submitted documents and their status.

### R2. Implement Use Case Tests
- `RegisterVendorProfileUseCaseTest`: Tests successful registration (status=PENDING, role update), duplicate registration (`VendorAlreadyExistsException`), and locked user prevention.
- `UpdateVendorProfileUseCaseTest`: Tests valid updates, mass assignment prevention (ignoring `verificationStatus`/`ratingAvg`), and cross-vendor manipulation (returning 403/404 by fetching vendor via userId from SecurityContext, not path param).
- `UploadVendorDocumentUseCaseTest`: Tests successful upload (status PENDING, `reviewed_by`/`reviewed_at` null), invalid `doc_type` not in enum (400), and upload before vendor registration (404/409).

### R3. Implement Controller Tests
- `VendorProfileControllerTest` (MockMvc): Tests 404/error for CUSTOMER without a vendor profile on GET, and 403 for ADMIN trying to access VENDOR routes.

## Acceptance Criteria

### Verification & Testing
- [ ] All specified test classes (`RegisterVendorProfileUseCaseTest`, `UpdateVendorProfileUseCaseTest`, `UploadVendorDocumentUseCaseTest`, `VendorProfileControllerTest`) are implemented and pass when running the test suite (e.g., via `mvn test` or `./gradlew test`).
- [ ] The `PATCH` endpoint uses a specific DTO that strictly excludes `verification_status`, `rating_avg`, and `badge_tier`.
- [ ] The vendor identity in the `PATCH` endpoint is derived directly from the `SecurityContext` (user ID), not from user-provided request parameters or path variables.
