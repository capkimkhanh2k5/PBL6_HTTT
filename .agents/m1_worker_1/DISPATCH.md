## 2026-09-10T03:56:15Z

You are a worker (m1_worker_1) responsible for implementing Milestone 1: Domain, Ports & Cross-Module Contracts.
Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_services_module/.agents/m1_worker_1
Workspace root: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_services_module
Requirements: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_services_module/.agents/ORIGINAL_REQUEST.md
Project plan: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_services_module/PROJECT.md

Investigation reports to follow:
- /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_services_module/.agents/m1_explorer_1/handoff.md
- /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_services_module/.agents/m1_explorer_2/handoff.md
- /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_services_module/.agents/m1_spec_miner_1/handoff.md

Scope & File Ownership for this milestone:
1. Domain Models (`backend/src/main/java/com/danasea/backend/modules/service/domain/models/`):
   - Enhance `Service.java`, `Category.java`, `ServiceImage.java` with `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`.
   - In `Service.java`, add `rejectionReason` (String) and domain methods for status validation/transition.
2. Domain Exceptions (`backend/src/main/java/com/danasea/backend/modules/service/domain/exceptions/`):
   - `ServiceDomainException.java`
   - `CategoryNotFoundException.java`
   - `CategoryInactiveException.java`
   - `WeatherRequirementsMissingException.java`
   - `ServiceImagesRequiredException.java`
   - `InvalidServiceStateException.java`
   - `VendorNotApprovedException.java`
   - `UnauthorizedServiceAccessException.java`
   - `ServiceNotFoundException.java`
3. Domain Ports (`backend/src/main/java/com/danasea/backend/modules/service/domain/ports/`):
   - `ServiceRepositoryPort.java`
   - `CategoryRepositoryPort.java`
   - `ServiceImageRepositoryPort.java`
   - `VendorPort.java`
   - `AuditLogPort.java`
4. Cross-Module Vendor Integration (`backend/src/main/java/com/danasea/backend/modules/vendor/`):
   - In `infrastructure/persistence/repositories/JpaVendorRepository.java`, add: `Optional<VendorJpaEntity> findByUserId(UUID userId);`
   - In `application/api/VendorInternalApi.java`, define methods:
     `Optional<Vendor> findByUserId(UUID userId);`
     `Optional<Vendor> findById(UUID vendorId);`
   - In `infrastructure/persistence/mapper/VendorMapper.java`, implement entity <-> domain mapping.
   - In `application/service/VendorInternalService.java`, implement `VendorInternalApi` as a Spring `@Service`.
5. Cross-Module Account / AuditLog Integration (`backend/src/main/java/com/danasea/backend/modules/account/`):
   - In `application/api/AccountInternalApi.java`, add:
     `void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata);`
   - In `application/service/AccountInternalService.java`, implement `recordAuditLog` using `JpaAuditLogRepository`.
6. Persistence Adapters & Mappers (`backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/`):
   - In `entities/ServiceJpaEntity.java`, add `rejectionReason` column.
   - In `repositories/JpaServiceRepository.java`, add:
     `List<ServiceJpaEntity> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);`
     `List<ServiceJpaEntity> findByVendorId(UUID vendorId);`
     `List<ServiceJpaEntity> findByStatusOrderByCreatedAtDesc(ServiceStatus status);`
     `List<ServiceJpaEntity> findByStatus(ServiceStatus status);`
   - In `repositories/JpaServiceImageRepository.java`, add:
     `List<ServiceImageJpaEntity> findByServiceIdOrderBySortOrderAsc(UUID serviceId);`
     `void deleteByServiceId(UUID serviceId);`
   - In `mappers/`: `ServiceMapper.java`, `CategoryMapper.java`, `ServiceImageMapper.java` with complete null safety.
   - In `adapters/`:
     `ServiceRepositoryAdapter.java` implements `ServiceRepositoryPort`
     `CategoryRepositoryAdapter.java` implements `CategoryRepositoryPort`
     `ServiceImageRepositoryAdapter.java` implements `ServiceImageRepositoryPort`
     `VendorAdapter.java` implements `VendorPort` (delegates to `VendorInternalApi`)
     `AuditLogAdapter.java` implements `AuditLogPort` (delegates to `AccountInternalApi`)
