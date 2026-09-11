# Project: Vendor Profile Module Implementation

## Architecture
Modular Monolith with Clean Architecture principles:
- **Domain Layer** (`com.danasea.backend.modules.vendor.domain`):
  - Models: `Vendor`, `VendorDocument`.
  - Enums: `VerificationStatus`, `DocStatus`, `DocType`, `BadgeTier`.
  - Exceptions: `VendorAlreadyExistsException`, `VendorNotFoundException`, `UserLockedException`, `InvalidDocTypeException`.
- **Application Layer** (`com.danasea.backend.modules.vendor.application`):
  - Ports: `VendorRepositoryPort`, `DocumentStoragePort`.
  - Use Cases:
    - `RegisterVendorProfileUseCase` (creates Vendor PENDING, sets User role to VENDOR via AccountInternalApi)
    - `GetVendorProfileUseCase` (gets Vendor by userId from context)
    - `UpdateVendorProfileUseCase` (strictly updates allowed fields, ignores status/rating/badge)
    - `UploadVendorDocumentUseCase` (uploads doc to Cloudinary via DocumentStoragePort, creates VendorDocument PENDING)
    - `GetVendorDocumentsUseCase` (retrieves vendor documents)
- **Infrastructure Layer** (`com.danasea.backend.modules.vendor.infrastructure`):
  - Persistence: `VendorJpaEntity`, `VendorDocumentJpaEntity`, `JpaVendorRepository`, `JpaVendorDocumentRepository`.
  - Storage: `CloudinaryDocumentStorageAdapter` implementing `DocumentStoragePort`.
- **Presentation Layer** (`com.danasea.backend.modules.vendor.presentation`):
  - DTOs: `RegisterVendorProfileRequest`, `UpdateVendorProfileRequest`, `VendorProfileResponse`, `VendorDocumentResponse`.
  - Controller: `VendorProfileController` with endpoints `/api/vendor/profile` and `/api/vendor/documents`.
  - Advice: `VendorExceptionHandler` mapping exceptions to standard `ErrorResponse`.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | POST /api/vendor/profile | CUSTOMER registers as Vendor; creates vendor record status PENDING, updates user role CUSTOMER -> VENDOR | M1 & M2 | ORIGINAL_REQUEST.md §R1 |
| 2 | GET /api/vendor/profile | Vendor views own profile via SecurityContext userId; 404 for CUSTOMER without profile, 403 for ADMIN | M1 & M2 | ORIGINAL_REQUEST.md §R1 |
| 3 | PATCH /api/vendor/profile | Vendor edits business_name, tax_code, address, bank_account_*; strict DTO prevents mass assignment | M1 & M2 | ORIGINAL_REQUEST.md §R1 |
| 4 | POST /api/vendor/documents | Uploads document (BUSINESS_LICENSE/SAFETY_CERT) to Cloudinary; creates vendor_documents record status PENDING | M1 & M2 | ORIGINAL_REQUEST.md §R1 |
| 5 | GET /api/vendor/documents | Vendor views submitted documents and review statuses | M1 & M2 | ORIGINAL_REQUEST.md §R1 |
| 6 | RegisterVendorProfileUseCaseTest | Unit tests for registration: PENDING status, role update, VendorAlreadyExistsException (409), locked user prevention (403) | M3 | ORIGINAL_REQUEST.md §R2 |
| 7 | UpdateVendorProfileUseCaseTest | Unit tests for update: valid updates, mass assignment prevention (status/rating), cross-vendor manipulation prevention | M3 | ORIGINAL_REQUEST.md §R2 |
| 8 | UploadVendorDocumentUseCaseTest | Unit tests for upload: PENDING status, reviewed_by/at null, invalid doc_type (400), upload before vendor registration (404/409) | M3 | ORIGINAL_REQUEST.md §R2 |
| 9 | VendorProfileControllerTest | MockMvc tests: 404 for CUSTOMER without profile on GET, 403 for ADMIN on VENDOR routes, 201 on registration, 400 on invalid input | M3 | ORIGINAL_REQUEST.md §R3 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Core Domain, Storage & Use Cases | Domain exceptions, storage port/adapter, 5 application use cases | Survey | DONE |
| 2 | M2: Presentation Layer & Security Integration | Request/Response DTOs, VendorProfileController, VendorExceptionHandler | M1 | DONE |
| 3 | M3: Comprehensive Unit & Controller Test Suite | RegisterVendorProfileUseCaseTest, UpdateVendorProfileUseCaseTest, UploadVendorDocumentUseCaseTest, VendorProfileControllerTest | M1, M2 | DONE |
| 4 | Phase 4: Review, Challenge & Forensic Audit | 2 Reviewers, 2 Challengers, 1 Auditor full verification | M1, M2, M3 | IN_PROGRESS |

## Interface Contracts
### VendorProfileController ↔ Use Cases
- `RegisterVendorProfileUseCase.execute(UUID userId, RegisterVendorProfileCommand command) -> Vendor`
- `GetVendorProfileUseCase.execute(UUID userId) -> Vendor`
- `UpdateVendorProfileUseCase.execute(UUID userId, UpdateVendorProfileCommand command) -> Vendor`
- `UploadVendorDocumentUseCase.execute(UUID userId, MultipartFile file, String docTypeStr) -> VendorDocument`
- `GetVendorDocumentsUseCase.execute(UUID userId) -> List<VendorDocument>`

### Vendor Module ↔ Account Module
- `AccountInternalApi.findUserById(UUID id) -> Optional<User>`
- `AccountInternalApi.findUserByEmail(String email) -> Optional<User>`
- `AccountInternalApi.saveUser(User user) -> User`

### Storage Port
- `DocumentStoragePort.uploadDocument(MultipartFile file, String folder) -> String (URL)`

## Code Layout
- `backend/src/main/java/com/danasea/backend/modules/vendor/domain/exception/`:
  - `VendorAlreadyExistsException.java`
  - `VendorNotFoundException.java`
  - `UserLockedException.java`
  - `InvalidDocTypeException.java`
- `backend/src/main/java/com/danasea/backend/modules/vendor/application/port/`:
  - `DocumentStoragePort.java`
- `backend/src/main/java/com/danasea/backend/modules/vendor/application/usecase/`:
  - `RegisterVendorProfileUseCase.java`
  - `GetVendorProfileUseCase.java`
  - `UpdateVendorProfileUseCase.java`
  - `UploadVendorDocumentUseCase.java`
  - `GetVendorDocumentsUseCase.java`
- `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/storage/`:
  - `CloudinaryDocumentStorageAdapter.java`
- `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/dto/`:
  - `RegisterVendorProfileRequest.java`
  - `UpdateVendorProfileRequest.java`
  - `VendorProfileResponse.java`
  - `VendorDocumentResponse.java`
- `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/controller/`:
  - `VendorProfileController.java`
- `backend/src/main/java/com/danasea/backend/modules/vendor/presentation/advice/`:
  - `VendorExceptionHandler.java`
- `backend/src/test/java/com/danasea/backend/modules/vendor/application/usecase/`:
  - `RegisterVendorProfileUseCaseTest.java`
  - `UpdateVendorProfileUseCaseTest.java`
  - `UploadVendorDocumentUseCaseTest.java`
- `backend/src/test/java/com/danasea/backend/modules/vendor/presentation/controller/`:
  - `VendorProfileControllerTest.java`
