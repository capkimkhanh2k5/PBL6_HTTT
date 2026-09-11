# Project: PBL6_HTTT — Service Assets & Safety Documents Module

## Architecture
- **Framework & Runtime**: Java 21 LTS, Spring Boot 4.1.1, Maven, PostgreSQL 17, Spring Data JPA, Spring Security JWT.
- **Pattern**: Modular Clean Architecture (`backend/docs/Clean_Architecture_Rules.md`).
- **Layers**:
  - `domain`: Pure models (`ServiceImage`, `ServiceSafetyDocument`, `Category`, `Service`), enums (`DocStatus`, `DocType`, `ServiceStatus`), domain exceptions.
  - `application`: Use cases (`UploadServiceImageUseCase`, `DeleteServiceImageUseCase`, `ReorderServiceImagesUseCase`, `UploadSafetyDocumentUseCase`, `ApproveSafetyDocumentUseCase`, `RejectSafetyDocumentUseCase`, `PublishServiceUseCase` / Safety Guard), ports (`FileStoragePort`).
  - `infrastructure`: Cloudinary adapter (`CloudinaryStorageAdapter`), Spring Data JPA entities & repositories (`JpaServiceImageRepository`, `JpaServiceSafetyDocumentRepository`, etc.), config beans.
  - `presentation`: REST Controllers (`VendorServiceImageController`, `VendorSafetyDocumentController`, `AdminSafetyDocumentController`), DTOs, exception handlers.

## Code Layout
- `backend/pom.xml`: Cloudinary dependency (`com.cloudinary:cloudinary-http44:1.39.0`).
- `backend/src/main/resources/application.yml`: Cloudinary configuration, multipart configuration.
- `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`: `mock-maker-subclass` for Java 21 sandbox execution.
- `backend/src/main/java/com/danasea/backend/modules/service/`:
  - `domain/models/`: `Category.java` (add `requiresSafetyCert`), `ServiceSafetyDocument.java` (add `rejectionReason`), `ServiceImage.java`, `Service.java`.
  - `domain/exceptions/`: `SafetyDocumentRequiredException`, `MaxImagesExceededException`, `InvalidFileTypeException`, `ServiceNotFoundException`, `UnauthorizedServiceAccessException`, `ImageNotFoundException`.
  - `application/ports/`: `FileStoragePort.java`.
  - `application/usecase/`:
    - `UploadServiceImageUseCase.java`
    - `DeleteServiceImageUseCase.java`
    - `ReorderServiceImagesUseCase.java`
    - `UploadSafetyDocumentUseCase.java`
    - `ApproveSafetyDocumentUseCase.java`
    - `RejectSafetyDocumentUseCase.java`
    - `GetSafetyDocumentsUseCase.java`
    - `CheckServiceSafetyComplianceUseCase.java` (Publish Guard)
  - `infrastructure/storage/`: `CloudinaryStorageAdapter.java`.
  - `infrastructure/persistence/`:
    - `entities/`: `CategoryJpaEntity.java` (add `requiresSafetyCert`), `ServiceSafetyDocumentJpaEntity.java` (add `rejectionReason`), `ServiceImageJpaEntity.java`, `ServiceJpaEntity.java`.
    - `repositories/`: `JpaServiceImageRepository.java`, `JpaServiceSafetyDocumentRepository.java`.
  - `presentation/controllers/`:
    - `VendorServiceImageController.java` (`/api/vendor/services/{id}/images`)
    - `VendorSafetyDocumentController.java` (`/api/vendor/services/{id}/safety-documents`)
    - `AdminSafetyDocumentController.java` (`/api/admin/services/{id}/safety-documents`)
- `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/`:
  - `UploadServiceImageUseCaseTest.java`
  - `ReorderServiceImagesUseCaseTest.java`
  - `UploadSafetyDocumentUseCaseTest.java`
  - `ApproveSafetyDocumentUseCaseTest.java`

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Cloudinary & File Storage Integration | Add Cloudinary SDK, application config, FileStoragePort, CloudinaryStorageAdapter, MockMaker for tests | M1 | Survey 1, Survey 2 |
| 2 | Upload Service Image API | `POST /api/vendor/services/{id}/images`: validate file, upload Cloudinary, auto-increment sort_order, max limit check, ownership check | M2 | R1, Survey 1 |
| 3 | Delete Service Image API | `DELETE /api/vendor/services/{id}/images/{imageId}`: remove image, ownership check | M2 | R1, Survey 1 |
| 4 | Batch Reorder Service Images API | `PATCH /api/vendor/services/{id}/images/reorder`: reorder sort_order, cross-service IDOR validation | M2 | R1, Survey 1 |
| 5 | Category Safety Certificate Flag | Add `requires_safety_cert` (boolean, default false) to Category model and JPA entity | M3 | R3, Survey 3 |
| 6 | Upload Safety Document API | `POST /api/vendor/services/{id}/safety-documents`: upload cert to Cloudinary, initial status PENDING, ownership check | M3 | R2, Survey 2 |
| 7 | Admin Get Safety Documents API | `GET /api/admin/services/{id}/safety-documents`: list documents for service | M3 | R2, Survey 2 |
| 8 | Admin Approve/Reject Safety Document API | `PATCH /api/admin/services/{id}/safety-documents/{docId}/approve|reject`: update status, audit reviewedBy/reviewedAt, rejectionReason | M3 | R2, Survey 2 |
| 9 | High-Risk Service Publish Guard | Block publishing service if (weather_sensitive=true or category.requires_safety_cert=true) and no APPROVED safety document exists | M4 | R3, Survey 2, Survey 3 |
| 10 | Acceptance Criteria Test Suites | UploadServiceImageUseCaseTest, ReorderServiceImagesUseCaseTest, UploadSafetyDocumentUseCaseTest, ApproveSafetyDocumentUseCaseTest | M5 | Acceptance Criteria, Survey 3 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Cloudinary & Testing Infra | Cloudinary SDK, config, FileStoragePort, CloudinaryStorageAdapter, MockMaker | none | PLANNED |
| M2 | Service Images Management | Upload, Delete, Batch Reorder APIs & Repository methods | M1 | PLANNED |
| M3 | Safety Documents & Category Config | Category flag, Upload, Get, Approve/Reject Safety Docs APIs & Repository methods | M1 | PLANNED |
| M4 | Business Rules & Publish Guard | Safety compliance check before PUBLISHED status | M2, M3 | PLANNED |
| M5 | Final Milestone: Test Suites & E2E Pass | 100% pass of unit test suites & E2E verification | M4 | PLANNED |

## Interface Contracts
### Application Layer ↔ Storage Adapter
- `FileStoragePort.uploadFile(byte[] fileData, String originalFilename, String folderPath): String` (returns secure URL)
- `FileStoragePort.deleteFile(String fileUrl): void`

### Service Images Repository
- `countByServiceId(UUID serviceId): long`
- `findByServiceIdOrderBySortOrderAsc(UUID serviceId): List<ServiceImageJpaEntity>`
- `findMaxSortOrderByServiceId(UUID serviceId): Optional<Short>`
- `findByIdAndServiceId(UUID id, UUID serviceId): Optional<ServiceImageJpaEntity>`
- `deleteByIdAndServiceId(UUID id, UUID serviceId): void`

### Safety Documents Repository
- `findByServiceId(UUID serviceId): List<ServiceSafetyDocumentJpaEntity>`
- `findByIdAndServiceId(UUID id, UUID serviceId): Optional<ServiceSafetyDocumentJpaEntity>`
- `existsByServiceIdAndStatus(UUID serviceId, DocStatus status): boolean`

### Publish Guard Contract
- `canPublish(Service service, Category category): boolean`
  - If `Boolean.TRUE.equals(service.getWeatherSensitive()) || Boolean.TRUE.equals(category.getRequiresSafetyCert())`:
    - Requires `existsByServiceIdAndStatus(service.getId(), DocStatus.APPROVED) == true`.
    - If false -> throws `SafetyDocumentRequiredException`.
