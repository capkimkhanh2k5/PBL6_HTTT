# Project: Vendor & Admin Services Module Implementation

## Architecture
- **Paradigm**: Modular Clean Architecture conforming to `backend/docs/Clean_Architecture_Rules.md`.
- **Layers**:
  - `domain`: Pure business entities (`Service`, `Category`, `ServiceImage`, `ServiceStatus`), domain exceptions, repository & service ports. No framework dependencies.
  - `application`: Use cases (`CreateServiceUseCase`, `SubmitServiceForReviewUseCase`, `UpdateServiceUseCase`, `ApproveServiceUseCase`, `RejectServiceUseCase`, `DeleteServiceUseCase`, `PauseServiceUseCase`, `ResumeServiceUseCase`, `GetVendorServicesUseCase`, `GetAdminServicesUseCase`), input commands, output results.
  - `infrastructure`: JPA entities, Spring Data JPA repositories, persistence adapters, entity mappers, Spring configuration beans (`ServiceBeans.java`).
  - `presentation`: REST Controllers (`VendorServiceController`, `AdminServiceController`), request/response DTO records, `ServiceExceptionHandler` mapping domain exceptions to `ErrorResponse`.
- **Cross-Module Communication**:
  - `service` module depends only on public abstractions / ports:
    - `VendorInternalApi` (in `vendor` module) or `VendorPort` to check vendor approval and query vendor by user ID.
    - `AccountInternalApi` (in `account` module) or `AuditLogPort` to log administrative actions (`SERVICE_APPROVED`, `SERVICE_REJECTED`).
- **Security & Authorization**:
  - Stateless JWT authentication via `JwtAuthenticationFilter`.
  - Roles mapped with `ROLE_` prefix (`ROLE_VENDOR`, `ROLE_ADMIN`, `ROLE_CUSTOMER`).
  - Method-level security (`@PreAuthorize("hasRole('VENDOR')")` and `@PreAuthorize("hasRole('ADMIN')")`).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Create Service | `POST /api/vendor/services`: Create service in DRAFT status, validate vendor APPROVED, category exists & active, weather rules | M1, M2 | ORIGINAL_REQUEST.md R1, R3 |
| 2 | Get Vendor Services | `GET /api/vendor/services`: List all services for authenticated vendor | M2 | ORIGINAL_REQUEST.md R1 |
| 3 | Get Service Detail | `GET /api/vendor/services/{id}`: Service details, owner only | M2 | ORIGINAL_REQUEST.md R1 |
| 4 | Update Service | `PATCH /api/vendor/services/{id}`: Update service fields, owner only; if PUBLISHED transitions to PENDING_REVIEW | M2 | ORIGINAL_REQUEST.md R1, R3 |
| 5 | Submit For Review | `POST /api/vendor/services/{id}/submit`: DRAFT/REJECTED -> PENDING_REVIEW, requires >= 1 image | M2 | ORIGINAL_REQUEST.md R1, R3 |
| 6 | Pause Service | `PATCH /api/vendor/services/{id}/pause`: PUBLISHED -> PAUSED, owner only | M2 | ORIGINAL_REQUEST.md R1 |
| 7 | Resume Service | `PATCH /api/vendor/services/{id}/resume`: PAUSED -> PUBLISHED, owner only | M2 | ORIGINAL_REQUEST.md R1 |
| 8 | Delete Draft Service | `DELETE /api/vendor/services/{id}`: Owner only, ONLY if status is DRAFT (400 if PUBLISHED/PAUSED/etc.) | M2 | ORIGINAL_REQUEST.md R1, R3 |
| 9 | List Pending Services | `GET /api/admin/services?status=PENDING_REVIEW`: Admin lists services pending review | M3 | ORIGINAL_REQUEST.md R2 |
| 10 | Approve Service | `PATCH /api/admin/services/{id}/approve`: PENDING_REVIEW -> PUBLISHED, records audit log SERVICE_APPROVED | M3 | ORIGINAL_REQUEST.md R2 |
| 11 | Reject Service | `PATCH /api/admin/services/{id}/reject`: PENDING_REVIEW -> REJECTED with mandatory reason, records audit log | M3 | ORIGINAL_REQUEST.md R2 |
| 12 | Business Rules Validation | Validate vendor approved, category active, weather rules, image required on submit, status guards | M1, M2, M3 | ORIGINAL_REQUEST.md R3 |
| 13 | Security & Access Control | `/api/vendor/**` 403 for CUSTOMER/ADMIN; `/api/admin/**` 403 for VENDOR/CUSTOMER; owner check 403 | M4 | ORIGINAL_REQUEST.md R4 |
| 14 | Audit Logging | Log SERVICE_APPROVED and SERVICE_REJECTED with actorUserId, entityType, metadata | M1, M3 | ORIGINAL_REQUEST.md R2 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Domain, Ports & Cross-Module Contracts | Domain exceptions, ports, mappers, VendorInternalApi / findByUserId, AuditLogPort | none | IN_PROGRESS |
| M2 | Vendor Use Cases & Unit Tests | Create, Update, Submit, Pause, Resume, Delete use cases and unit tests (CreateServiceUseCaseTest, SubmitServiceForReviewUseCaseTest, UpdateServiceUseCaseTest, DeleteServiceUseCaseTest) | M1 | PLANNED |
| M3 | Admin Use Cases & Unit Tests | Approve, Reject, List Pending use cases, audit log recording, unit tests (ApproveServiceUseCaseTest, RejectServiceUseCaseTest) | M1 | PLANNED |
| M4 | REST Controllers, DTOs & MockMvc Tests | VendorServiceController, AdminServiceController, ServiceExceptionHandler, ServiceControllerTest (MockMvc security & RBAC) | M2, M3 | PLANNED |
| M5 | Final Milestone: E2E Test Pass & Hardening | Pass 100% E2E test suite (Tiers 1-4 from TEST_READY.md) + Adversarial Coverage Hardening | M4 | PLANNED |

## Interface Contracts

### `service` ↔ `vendor`
- **Port / API**: `VendorPort` or `VendorInternalApi`
- **Methods**:
  - `Optional<Vendor> findByUserId(UUID userId)`
  - `Optional<Vendor> findById(UUID vendorId)`
- **Behavior**:
  - If Vendor not found or `vendor.verificationStatus() != VerificationStatus.APPROVED`, reject service creation with `VendorNotApprovedException` (HTTP 400/403).

### `service` ↔ `account`
- **Port / API**: `AuditLogPort` or `AccountInternalApi`
- **Methods**:
  - `void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata)`
- **Behavior**:
  - Persists audit log record when Admin approves (`SERVICE_APPROVED`) or rejects (`SERVICE_REJECTED`).

### Domain Repository Ports in `modules/service/domain/ports/`
- `ServiceRepositoryPort`:
  - `Service save(Service service)`
  - `Optional<Service> findById(UUID id)`
  - `List<Service> findByVendorId(UUID vendorId)`
  - `List<Service> findByStatus(ServiceStatus status)`
  - `void deleteById(UUID id)`
- `CategoryRepositoryPort`:
  - `Optional<Category> findById(UUID id)`
- `ServiceImageRepositoryPort`:
  - `List<ServiceImage> findByServiceId(UUID serviceId)`
  - `List<ServiceImage> saveAll(List<ServiceImage> images)`

## Code Layout
- `backend/src/main/java/com/danasea/backend/modules/service/`:
  - `domain/`:
    - `models/`: `Service.java`, `Category.java`, `ServiceImage.java`, `ServiceStatus.java`
    - `exceptions/`: `ServiceNotFoundException.java`, `CategoryNotFoundException.java`, `CategoryInactiveException.java`, `WeatherRequirementsMissingException.java`, `ServiceImagesRequiredException.java`, `InvalidServiceStateException.java`, `VendorNotApprovedException.java`, `UnauthorizedServiceAccessException.java`
    - `ports/`: `ServiceRepositoryPort.java`, `CategoryRepositoryPort.java`, `ServiceImageRepositoryPort.java`, `VendorPort.java`, `AuditLogPort.java`
  - `application/`:
    - `usecases/`: `CreateServiceUseCase.java`, `SubmitServiceForReviewUseCase.java`, `UpdateServiceUseCase.java`, `ApproveServiceUseCase.java`, `RejectServiceUseCase.java`, `DeleteServiceUseCase.java`, `PauseServiceUseCase.java`, `ResumeServiceUseCase.java`, `GetVendorServicesUseCase.java`, `GetAdminServicesUseCase.java`
    - `dto/`: commands and query results
  - `infrastructure/`:
    - `persistence/`:
      - `entities/`: `ServiceJpaEntity.java`, `CategoryJpaEntity.java`, `ServiceImageJpaEntity.java`
      - `repositories/`: `JpaServiceRepository.java`, `JpaCategoryRepository.java`, `JpaServiceImageRepository.java`
      - `adapters/`: `ServiceRepositoryAdapter.java`, `CategoryRepositoryAdapter.java`, `ServiceImageRepositoryAdapter.java`, `VendorAdapter.java`, `AuditLogAdapter.java`
      - `mappers/`: `ServiceMapper.java`, `ServiceImageMapper.java`, `CategoryMapper.java`
    - `config/`: `ServiceBeans.java`
  - `presentation/`:
    - `controllers/`: `VendorServiceController.java`, `AdminServiceController.java`
    - `dto/`: request & response records
    - `handlers/`: `ServiceExceptionHandler.java`
- `backend/src/main/java/com/danasea/backend/modules/vendor/`:
  - `application/api/`: `VendorInternalApi.java`
  - `infrastructure/persistence/repositories/JpaVendorRepository.java` (add `findByUserId`)
- `backend/src/main/java/com/danasea/backend/modules/account/`:
  - `application/api/AccountInternalApi.java` (add audit log method if needed)
- `backend/src/test/java/com/danasea/backend/modules/service/`:
  - `application/usecases/`:
    - `CreateServiceUseCaseTest.java`
    - `SubmitServiceForReviewUseCaseTest.java`
    - `UpdateServiceUseCaseTest.java`
    - `ApproveServiceUseCaseTest.java`
    - `RejectServiceUseCaseTest.java`
    - `DeleteServiceUseCaseTest.java`
  - `presentation/controllers/`:
    - `ServiceControllerTest.java`
