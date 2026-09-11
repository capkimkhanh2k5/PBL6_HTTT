# Handoff Report: Specification Mining for Service Persistence Adapters & Mappers (M1)

## Features Discovered
| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Persistence Entities | `ServiceJpaEntity` mapping | JPA entity mapped to table `services` extending `BaseJpaEntity` | JPA columns (UUID, strings, numeric, enums) | Persisted entity | DataIntegrityViolationException on constraint failure | `ServiceJpaEntity.java`, `BaseJpaEntity.java` |
| 2 | Persistence Entities | `CategoryJpaEntity` mapping | JPA entity mapped to table `categorys` extending `BaseJpaEntity` | JPA columns (`name`, `nameEn`, `slug`, `parentId`, `iconUrl`, `isActive`) | Persisted entity | DataIntegrityViolationException | `CategoryJpaEntity.java` |
| 3 | Persistence Entities | `ServiceImageJpaEntity` mapping | JPA entity mapped to table `service_images` extending `BaseJpaEntity` | JPA columns (`serviceId`, `url`, `sortOrder`) | Persisted entity | DataIntegrityViolationException | `ServiceImageJpaEntity.java` |
| 4 | Spring Data Repositories | `JpaServiceRepository` queries | Repository queries for service persistence and lookup | `UUID vendorId`, `ServiceStatus status`, `UUID id` | `List<ServiceJpaEntity>`, `Optional<ServiceJpaEntity>` | Empty optional / empty list if not found | `JpaServiceRepository.java`, `PROJECT.md` |
| 5 | Spring Data Repositories | `JpaCategoryRepository` queries | Repository queries for category lookup and validation | `UUID categoryId` | `Optional<CategoryJpaEntity>` | Empty optional if not found | `JpaCategoryRepository.java` |
| 6 | Spring Data Repositories | `JpaServiceImageRepository` queries | Repository queries for service image lookup ordered by sort order | `UUID serviceId` | `List<ServiceImageJpaEntity>` ordered by `sortOrder ASC` | Empty list if no images | `JpaServiceImageRepository.java` |
| 7 | Mappers | `ServiceMapper` | Bi-directional mapping between domain `Service` and `ServiceJpaEntity` | Domain `Service` or Entity `ServiceJpaEntity` | Corresponding mapped object or list | Returns null if input is null | `UserMapper.java` pattern, `Service.java` |
| 8 | Mappers | `CategoryMapper` | Bi-directional mapping between domain `Category` and `CategoryJpaEntity` | Domain `Category` or Entity `CategoryJpaEntity` | Corresponding mapped object or list | Returns null if input is null | `Category.java`, `CategoryJpaEntity.java` |
| 9 | Mappers | `ServiceImageMapper` | Bi-directional mapping between domain `ServiceImage` and `ServiceImageJpaEntity` | Domain `ServiceImage` or Entity `ServiceImageJpaEntity` | Corresponding mapped object or list | Returns null if input is null | `ServiceImage.java`, `ServiceImageJpaEntity.java` |
| 10 | Persistence Adapter | `ServiceRepositoryAdapter` | Implementation of `ServiceRepositoryPort` delegating to `JpaServiceRepository` and `ServiceMapper` | Domain `Service`, `UUID id`, `UUID vendorId`, `ServiceStatus status` | Domain `Service`, `Optional<Service>`, `List<Service>` | Propagates data exceptions, returns empty optional/list | `PROJECT.md`, `Clean_Architecture_Rules.md` |
| 11 | Persistence Adapter | `CategoryRepositoryAdapter` | Implementation of `CategoryRepositoryPort` delegating to `JpaCategoryRepository` and `CategoryMapper` | `UUID categoryId` | `Optional<Category>` | Returns `Optional.empty()` if not found | `PROJECT.md`, `Clean_Architecture_Rules.md` |
| 12 | Persistence Adapter | `ServiceImageRepositoryAdapter` | Implementation of `ServiceImageRepositoryPort` delegating to `JpaServiceImageRepository` and `ServiceImageMapper` | `UUID serviceId`, `List<ServiceImage>` | `List<ServiceImage>` | Empty list if no images | `PROJECT.md`, `Clean_Architecture_Rules.md` |
| 13 | Cross-Module Adapter | `VendorAdapter` | Implementation of `VendorPort` delegating to `VendorInternalApi` | `UUID userId` or `UUID vendorId` | `Optional<Vendor>` | Returns `Optional.empty()` if vendor not found | `PROJECT.md`, `VendorJpaEntity.java` |
| 14 | Cross-Module Adapter | `AuditLogAdapter` | Implementation of `AuditLogPort` delegating to `AccountInternalApi` | `UUID actorUserId`, `String action`, `String entityType`, `UUID entityId`, `String metadata` | void | Propagates runtime exceptions | `PROJECT.md`, `AccountInternalApi.java` |

## Edge Cases
| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | `ServiceMapper.toDomain` | `null` entity | Returns `null` without throwing NullPointerException |
| 2 | `ServiceMapper.toEntity` | `null` domain model | Returns `null` without throwing NullPointerException |
| 3 | `ServiceMapper.toDomainList` | `null` or empty list | Returns `Collections.emptyList()` |
| 4 | `ServiceMapper.toEntity` | `status` is null | Sets default status `ServiceStatus.DRAFT` |
| 5 | `ServiceMapper.toEntity` | `weatherSensitive` is null | Defaults `weatherSensitive` to `Boolean.FALSE` |
| 6 | `ServiceMapper.toEntity` | `viewCount` / `ratingCount` is null | Defaults integer counts to `0` to prevent unboxing NPEs |
| 7 | `ServiceImageMapper.toEntity` | `sortOrder` is null | Defaults `sortOrder` to `(short) 0` |
| 8 | `CategoryRepositoryAdapter.findById` | Unknown `UUID` | Returns `Optional.empty()`, enabling use cases to throw `CategoryNotFoundException` |
| 9 | `Category.getIsActive` validation | Category exists but `isActive == false` or `null` | Domain validation detects inactive category and throws `CategoryInactiveException` |
| 10 | `VendorPort.findByUserId` | User without Vendor record | Returns `Optional.empty()`, enabling use cases to throw `VendorNotApprovedException` |
| 11 | `Vendor.verificationStatus` | Vendor found but `verificationStatus != APPROVED` (e.g. `PENDING`, `REJECTED`) | Domain validation throws `VendorNotApprovedException` |
| 12 | `JpaServiceImageRepository.findByServiceId` | Multiple images with different sort orders | Returns images sorted ascending by `sortOrder` (`findByServiceIdOrderBySortOrderAsc`) |
| 13 | Delete Draft Service with images | Service in `DRAFT` deleted | `ServiceImageRepositoryPort.deleteByServiceId(serviceId)` removes associated images to avoid orphaned rows |
| 14 | Update service when `PUBLISHED` | Update payload on published service | State transitions to `PENDING_REVIEW` per R3; mapper persists new status properly |

---

## 1. Observation
1. **Existing JPA Entities** (`backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/`):
   - `ServiceJpaEntity.java` (lines 16-63): extends `BaseJpaEntity`, annotated with `@Entity`, `@Table(name = "services")`, `@Getter`, `@Setter`. Contains fields:
     - `vendorId`: `UUID`
     - `categoryId`: `UUID`
     - `name`: `String`, `nameEn`: `String`, `slug`: `String`
     - `description`: `String`, `descriptionEn`: `String`
     - `price`: `BigDecimal`, `durationMinutes`: `Integer`, `capacityPerSlot`: `Integer`
     - `locationName`: `String`, `address`: `String`, `latitude`: `BigDecimal`, `longitude`: `BigDecimal`
     - `status`: `ServiceStatus` (`@Enumerated(EnumType.STRING)`)
     - `waiverContent`: `String`
     - `weatherSensitive`: `Boolean`
     - `minWindKmh`: `BigDecimal`, `maxWaveM`: `BigDecimal`
     - `avgRating`: `BigDecimal`, `ratingCount`: `Integer`, `viewCount`: `Integer`
     - Inherited from `BaseJpaEntity`: `id` (`UUID`, `@GeneratedValue(strategy = GenerationType.UUID)`), `createdAt` (`OffsetDateTime`, `@CreationTimestamp`, `updatable = false`), `updatedAt` (`OffsetDateTime`, `@UpdateTimestamp`).
   - `CategoryJpaEntity.java` (lines 14-28): extends `BaseJpaEntity`, annotated with `@Table(name = "categorys")` (note table name). Fields: `name`: `String`, `nameEn`: `String`, `slug`: `String`, `parentId`: `UUID`, `iconUrl`: `String`, `isActive`: `Boolean`.
   - `ServiceImageJpaEntity.java` (lines 14-22): extends `BaseJpaEntity`, annotated with `@Table(name = "service_images")`. Fields: `serviceId`: `UUID`, `url`: `String`, `sortOrder`: `Short`.
   - **Crucial Architecture Observation**: Notice there is NO `@ManyToOne` or `@OneToMany` relationship annotations inside `ServiceJpaEntity` referencing `ServiceImageJpaEntity` or `CategoryJpaEntity`. Relationships are maintained via direct foreign key scalar fields (`UUID serviceId`, `UUID categoryId`, `UUID vendorId`). This enforces loose coupling at the database mapping layer and requires repository queries to manage related entities explicitly.

2. **Existing Spring Data Repositories** (`backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/`):
   - `JpaServiceRepository.java`: lines 11-12: `public interface JpaServiceRepository extends JpaRepository<ServiceJpaEntity, UUID> {}` (presently has 0 declared query methods).
   - `JpaCategoryRepository.java`: lines 11-12: `public interface JpaCategoryRepository extends JpaRepository<CategoryJpaEntity, UUID> {}` (presently has 0 declared query methods).
   - `JpaServiceImageRepository.java`: lines 11-12: `public interface JpaServiceImageRepository extends JpaRepository<ServiceImageJpaEntity, UUID> {}` (presently has 0 declared query methods).

3. **Existing Domain Models** (`backend/src/main/java/com/danasea/backend/modules/service/domain/models/`):
   - `Service.java`: extends `BaseDomainModel` (`id`, `createdAt`, `updatedAt`), annotated with `@Data`, `@EqualsAndHashCode(callSuper = true)`. Fields exactly mirror `ServiceJpaEntity` 1-to-1 in names and types.
   - `Category.java`: extends `BaseDomainModel`, fields exactly mirror `CategoryJpaEntity` 1-to-1.
   - `ServiceImage.java`: extends `BaseDomainModel`, fields exactly mirror `ServiceImageJpaEntity` 1-to-1 (`serviceId`: `UUID`, `url`: `String`, `sortOrder`: `Short`).
   - `ServiceStatus.java`: enum with values `DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `REJECTED`, `PAUSED`.

4. **Reference Implementation Patterns**:
   - `UserMapper.java` (`modules/account/infrastructure/mapper/UserMapper.java`): Uses Spring `@Component`, explicit manual setters/getters, null checks `if (entity == null) return null;` and `if (domain == null) return null;`.
   - `UserAccountAdapter.java` (`security/authentication/infrastructure/persistence/UserAccountAdapter.java`): Uses Spring `@Component`, `@RequiredArgsConstructor`, implements port interface (`UserAccountPort`), uses cross-module API (`AccountInternalApi`), converts domain to entity and vice versa, and handles exceptions cleanly.
   - `ApplicationBeans.java` (`config/ApplicationBeans.java`): Declares use cases as Spring `@Bean` definitions by wiring port interfaces into use case constructors.

5. **Cross-Module Boundaries**:
   - `modules/vendor`:
     - `Vendor.java`: domain model with `UUID userId`, `VerificationStatus verificationStatus` (`PENDING`, `APPROVED`, `REJECTED`), `UUID id`.
     - `JpaVendorRepository.java`: currently empty `JpaRepository<VendorJpaEntity, UUID>`.
     - `VendorInternalApi.java` and `VendorInternalService.java`: not yet present.
   - `modules/account`:
     - `AuditLog.java`: domain model with `actorUserId`, `action`, `entityType`, `entityId`, `metadata`.
     - `AuditLogJpaEntity.java`: `@Table(name = "audit_logs")`, fields `actorUserId`, `action`, `entityType`, `entityId`, `metadata`.
     - `JpaAuditLogRepository.java`: extends `JpaRepository<AuditLogJpaEntity, UUID>`.
     - `AccountInternalApi.java` & `AccountInternalService.java`: existing cross-module API, currently lacks `recordAuditLog(...)`.

---

## 2. Logic Chain
1. **Repository Queries Requirement**:
   - For Vendor operations:
     - Use case `GetVendorServicesUseCase` requires finding all services owned by a specific vendor -> `findByVendorId(UUID vendorId)`.
     - To ensure intuitive display in vendor UI, sorting by creation timestamp descending (`findByVendorIdOrderByCreatedAtDesc`) is recommended.
   - For Admin operations:
     - Use case `GetAdminServicesUseCase` requires listing services in `PENDING_REVIEW` -> `findByStatus(ServiceStatus status)` or `findByStatusOrderByCreatedAtDesc(ServiceStatus status)`.
     - Also supporting general lookup: `findAll()` when status parameter is omitted.
   - For Images:
     - Use case `SubmitServiceForReviewUseCase` must verify that at least one image is uploaded (`count >= 1`).
     - Detail queries need images in display order -> `findByServiceIdOrderBySortOrderAsc(UUID serviceId)`.
     - Service deletion requires deleting associated images -> `deleteByServiceId(UUID serviceId)`.
   - For Categories:
     - `CreateServiceUseCase` and `UpdateServiceUseCase` must verify that the category exists and is active -> `findById(UUID id)`.

2. **Mappers Design & Null Safety**:
   - Both domain models (`Service`, `Category`, `ServiceImage`) and JPA entities (`ServiceJpaEntity`, `CategoryJpaEntity`, `ServiceImageJpaEntity`) share the exact same property names and types.
   - Using explicit `@Component` mappers ensures complete compile-time type safety, avoiding reflection overhead or bytecode issues from MapStruct/ModelMapper.
   - Null handling:
     - `toDomain(null)` -> returns `null`.
     - `toEntity(null)` -> returns `null`.
     - `toDomainList(null)` -> returns `Collections.emptyList()`.
     - `toEntityList(null)` -> returns `Collections.emptyList()`.
   - Primitive wrapper safety:
     - `weatherSensitive`: default to `Boolean.FALSE` if null in `toEntity`. In business logic, use `Boolean.TRUE.equals(...)` to prevent `NullPointerException` during unboxing.
     - `sortOrder`: `Short` type in both entity and domain. Default to `(short) 0` if null.
     - `viewCount` & `ratingCount`: default to `0` if null.
     - `avgRating`: can remain null or `BigDecimal.ZERO`.
     - `status`: default to `ServiceStatus.DRAFT` if null when persisting a new entity.

3. **Adapters and Port Contracts**:
   - Following Clean Architecture Rule 11 (Module Dependency Rule), the `service` module must not depend on concrete repositories or services of `vendor` or `account` modules.
   - `ServiceRepositoryAdapter`:
     - Implements `ServiceRepositoryPort`.
     - Injects `JpaServiceRepository` and `ServiceMapper`.
     - Wraps calls cleanly, converting domain <-> entity.
   - `CategoryRepositoryAdapter`:
     - Implements `CategoryRepositoryPort`.
     - Injects `JpaCategoryRepository` and `CategoryMapper`.
     - Returns `Optional<Category>`.
   - `ServiceImageRepositoryAdapter`:
     - Implements `ServiceImageRepositoryPort`.
     - Injects `JpaServiceImageRepository` and `ServiceImageMapper`.
     - Handles `findByServiceId`, `saveAll`, `deleteByServiceId`, `deleteById`.
   - `VendorAdapter`:
     - Implements `VendorPort` (defined in `com.danasea.backend.modules.service.domain.ports`).
     - Injects `VendorInternalApi` (defined in `com.danasea.backend.modules.vendor.application.api`).
     - Provides `findByUserId(UUID userId)` and `findById(UUID vendorId)`.
     - This isolates the `service` module from the vendor persistence schema and vendor table changes.
   - `AuditLogAdapter`:
     - Implements `AuditLogPort` (defined in `com.danasea.backend.modules.service.domain.ports`).
     - Injects `AccountInternalApi` (defined in `com.danasea.backend.modules.account.application.api`).
     - Calls `recordAuditLog(...)` to log administrative actions `SERVICE_APPROVED` and `SERVICE_REJECTED`.

---

## 3. Caveats
1. **Category Table Naming**: The entity `CategoryJpaEntity` has `@Table(name = "categorys")`. Note the spelling with "ys", not "ies". Any native queries or schema scripts must adhere to `categorys`.
2. **Loosely Coupled Relations**: There are no JPA cascade delete rules between `ServiceJpaEntity` and `ServiceImageJpaEntity`. Deletion of a service must explicitly invoke `serviceImageRepository.deleteByServiceId(serviceId)` within the application/adapter transaction, or database FK cascade must be relied upon. Explicit adapter invocation is recommended for safety.
3. **Database Schema Auto-Update**: `application.yml` has `spring.jpa.hibernate.ddl-auto: update`. When adding new query methods, no manual DDL migrations are strictly required, but entity definitions dictate table schemas.
4. **Vendor and Account Module Additions**: For `VendorAdapter` and `AuditLogAdapter` to compile and function, the corresponding public APIs in `modules/vendor` (`VendorInternalApi`, `findByUserId`) and `modules/account` (`recordAuditLog`) must be added as part of Milestone 1.

---

## 4. Conclusion & Concrete Implementation Specification

### 4.1. Spring Data JPA Repositories Specification
#### `JpaServiceRepository.java`
Path: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceRepository.java`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaServiceRepository extends JpaRepository<ServiceJpaEntity, UUID> {
    List<ServiceJpaEntity> findByVendorId(UUID vendorId);
    List<ServiceJpaEntity> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);
    List<ServiceJpaEntity> findByStatus(ServiceStatus status);
    List<ServiceJpaEntity> findByStatusOrderByCreatedAtDesc(ServiceStatus status);
    boolean existsBySlug(String slug);
}
```

#### `JpaCategoryRepository.java`
Path: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaCategoryRepository.java`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import com.danasea.backend.modules.service.infrastructure.persistence.entities.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCategoryRepository extends JpaRepository<CategoryJpaEntity, UUID> {
    Optional<CategoryJpaEntity> findByIdAndIsActiveTrue(UUID id);
}
```

#### `JpaServiceImageRepository.java`
Path: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceImageRepository.java`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaServiceImageRepository extends JpaRepository<ServiceImageJpaEntity, UUID> {
    List<ServiceImageJpaEntity> findByServiceId(UUID serviceId);
    List<ServiceImageJpaEntity> findByServiceIdOrderBySortOrderAsc(UUID serviceId);
    void deleteByServiceId(UUID serviceId);
}
```

#### `JpaVendorRepository.java` (in `modules/vendor`)
Path: `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/repositories/JpaVendorRepository.java`
```java
package com.danasea.backend.modules.vendor.infrastructure.persistence.repositories;

import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaVendorRepository extends JpaRepository<VendorJpaEntity, UUID> {
    Optional<VendorJpaEntity> findByUserId(UUID userId);
}
```

---

### 4.2. Mappers Specification

#### `ServiceMapper.java`
Path: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/mappers/ServiceMapper.java`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.mappers;

import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class ServiceMapper {

    public Service toDomain(ServiceJpaEntity entity) {
        if (entity == null) return null;
        Service domain = new Service();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setVendorId(entity.getVendorId());
        domain.setCategoryId(entity.getCategoryId());
        domain.setName(entity.getName());
        domain.setNameEn(entity.getNameEn());
        domain.setSlug(entity.getSlug());
        domain.setDescription(entity.getDescription());
        domain.setDescriptionEn(entity.getDescriptionEn());
        domain.setPrice(entity.getPrice());
        domain.setDurationMinutes(entity.getDurationMinutes());
        domain.setCapacityPerSlot(entity.getCapacityPerSlot());
        domain.setLocationName(entity.getLocationName());
        domain.setAddress(entity.getAddress());
        domain.setLatitude(entity.getLatitude());
        domain.setLongitude(entity.getLongitude());
        domain.setStatus(entity.getStatus());
        domain.setWaiverContent(entity.getWaiverContent());
        domain.setWeatherSensitive(entity.getWeatherSensitive());
        domain.setMinWindKmh(entity.getMinWindKmh());
        domain.setMaxWaveM(entity.getMaxWaveM());
        domain.setAvgRating(entity.getAvgRating());
        domain.setRatingCount(entity.getRatingCount());
        domain.setViewCount(entity.getViewCount());
        return domain;
    }

    public ServiceJpaEntity toEntity(Service domain) {
        if (domain == null) return null;
        ServiceJpaEntity entity = new ServiceJpaEntity();
        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVendorId(domain.getVendorId());
        entity.setCategoryId(domain.getCategoryId());
        entity.setName(domain.getName());
        entity.setNameEn(domain.getNameEn());
        entity.setSlug(domain.getSlug());
        entity.setDescription(domain.getDescription());
        entity.setDescriptionEn(domain.getDescriptionEn());
        entity.setPrice(domain.getPrice());
        entity.setDurationMinutes(domain.getDurationMinutes());
        entity.setCapacityPerSlot(domain.getCapacityPerSlot());
        entity.setLocationName(domain.getLocationName());
        entity.setAddress(domain.getAddress());
        entity.setLatitude(domain.getLatitude());
        entity.setLongitude(domain.getLongitude());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus() : ServiceStatus.DRAFT);
        entity.setWaiverContent(domain.getWaiverContent());
        entity.setWeatherSensitive(domain.getWeatherSensitive() != null ? domain.getWeatherSensitive() : Boolean.FALSE);
        entity.setMinWindKmh(domain.getMinWindKmh());
        entity.setMaxWaveM(domain.getMaxWaveM());
        entity.setAvgRating(domain.getAvgRating() != null ? domain.getAvgRating() : BigDecimal.ZERO);
        entity.setRatingCount(domain.getRatingCount() != null ? domain.getRatingCount() : 0);
        entity.setViewCount(domain.getViewCount() != null ? domain.getViewCount() : 0);
        return entity;
    }

    public List<Service> toDomainList(List<ServiceJpaEntity> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream()
                .map(this::toDomain)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<ServiceJpaEntity> toEntityList(List<Service> domains) {
        if (domains == null) return Collections.emptyList();
        return domains.stream()
                .map(this::toEntity)
                .filter(Objects::nonNull)
                .toList();
    }
}
```

#### `CategoryMapper.java`
Path: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/mappers/CategoryMapper.java`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.mappers;

import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.CategoryJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class CategoryMapper {

    public Category toDomain(CategoryJpaEntity entity) {
        if (entity == null) return null;
        Category domain = new Category();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setName(entity.getName());
        domain.setNameEn(entity.getNameEn());
        domain.setSlug(entity.getSlug());
        domain.setParentId(entity.getParentId());
        domain.setIconUrl(entity.getIconUrl());
        domain.setIsActive(entity.getIsActive());
        return domain;
    }

    public CategoryJpaEntity toEntity(Category domain) {
        if (domain == null) return null;
        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setName(domain.getName());
        entity.setNameEn(domain.getNameEn());
        entity.setSlug(domain.getSlug());
        entity.setParentId(domain.getParentId());
        entity.setIconUrl(domain.getIconUrl());
        entity.setIsActive(domain.getIsActive() != null ? domain.getIsActive() : Boolean.TRUE);
        return entity;
    }

    public List<Category> toDomainList(List<CategoryJpaEntity> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream()
                .map(this::toDomain)
                .filter(Objects::nonNull)
                .toList();
    }
}
```

#### `ServiceImageMapper.java`
Path: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/mappers/ServiceImageMapper.java`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.mappers;

import com.danasea.backend.modules.service.domain.models.ServiceImage;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class ServiceImageMapper {

    public ServiceImage toDomain(ServiceImageJpaEntity entity) {
        if (entity == null) return null;
        ServiceImage domain = new ServiceImage();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setServiceId(entity.getServiceId());
        domain.setUrl(entity.getUrl());
        domain.setSortOrder(entity.getSortOrder());
        return domain;
    }

    public ServiceImageJpaEntity toEntity(ServiceImage domain) {
        if (domain == null) return null;
        ServiceImageJpaEntity entity = new ServiceImageJpaEntity();
        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setServiceId(domain.getServiceId());
        entity.setUrl(domain.getUrl());
        entity.setSortOrder(domain.getSortOrder() != null ? domain.getSortOrder() : (short) 0);
        return entity;
    }

    public List<ServiceImage> toDomainList(List<ServiceImageJpaEntity> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream()
                .map(this::toDomain)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<ServiceImageJpaEntity> toEntityList(List<ServiceImage> domains) {
        if (domains == null) return Collections.emptyList();
        return domains.stream()
                .map(this::toEntity)
                .filter(Objects::nonNull)
                .toList();
    }
}
```

---

### 4.3. Domain Repository Ports & Adapters Specification

#### `ServiceRepositoryPort.java` & `ServiceRepositoryAdapter.java`
Port: `com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort`
```java
package com.danasea.backend.modules.service.domain.ports;

import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepositoryPort {
    Service save(Service service);
    Optional<Service> findById(UUID id);
    List<Service> findByVendorId(UUID vendorId);
    List<Service> findByStatus(ServiceStatus status);
    List<Service> findAll();
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
```

Adapter: `com.danasea.backend.modules.service.infrastructure.persistence.adapters.ServiceRepositoryAdapter`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.mappers.ServiceMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceRepositoryAdapter implements ServiceRepositoryPort {

    private final JpaServiceRepository jpaServiceRepository;
    private final ServiceMapper serviceMapper;

    @Override
    public Service save(Service service) {
        ServiceJpaEntity entity = serviceMapper.toEntity(service);
        ServiceJpaEntity saved = jpaServiceRepository.save(entity);
        return serviceMapper.toDomain(saved);
    }

    @Override
    public Optional<Service> findById(UUID id) {
        return jpaServiceRepository.findById(id).map(serviceMapper::toDomain);
    }

    @Override
    public List<Service> findByVendorId(UUID vendorId) {
        List<ServiceJpaEntity> entities = jpaServiceRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
        return serviceMapper.toDomainList(entities);
    }

    @Override
    public List<Service> findByStatus(ServiceStatus status) {
        List<ServiceJpaEntity> entities = jpaServiceRepository.findByStatusOrderByCreatedAtDesc(status);
        return serviceMapper.toDomainList(entities);
    }

    @Override
    public List<Service> findAll() {
        return serviceMapper.toDomainList(jpaServiceRepository.findAll());
    }

    @Override
    public void deleteById(UUID id) {
        jpaServiceRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaServiceRepository.existsById(id);
    }
}
```

#### `CategoryRepositoryPort.java` & `CategoryRepositoryAdapter.java`
Port: `com.danasea.backend.modules.service.domain.ports.CategoryRepositoryPort`
```java
package com.danasea.backend.modules.service.domain.ports;

import com.danasea.backend.modules.service.domain.models.Category;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepositoryPort {
    Optional<Category> findById(UUID id);
}
```

Adapter: `com.danasea.backend.modules.service.infrastructure.persistence.adapters.CategoryRepositoryAdapter`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.domain.ports.CategoryRepositoryPort;
import com.danasea.backend.modules.service.infrastructure.persistence.mappers.CategoryMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepositoryPort {

    private final JpaCategoryRepository jpaCategoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaCategoryRepository.findById(id).map(categoryMapper::toDomain);
    }
}
```

#### `ServiceImageRepositoryPort.java` & `ServiceImageRepositoryAdapter.java`
Port: `com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort`
```java
package com.danasea.backend.modules.service.domain.ports;

import com.danasea.backend.modules.service.domain.models.ServiceImage;

import java.util.List;
import java.util.UUID;

public interface ServiceImageRepositoryPort {
    List<ServiceImage> findByServiceId(UUID serviceId);
    List<ServiceImage> saveAll(List<ServiceImage> images);
    void deleteByServiceId(UUID serviceId);
    void deleteById(UUID id);
}
```

Adapter: `com.danasea.backend.modules.service.infrastructure.persistence.adapters.ServiceImageRepositoryAdapter`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import com.danasea.backend.modules.service.domain.models.ServiceImage;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.mappers.ServiceImageMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceImageRepositoryAdapter implements ServiceImageRepositoryPort {

    private final JpaServiceImageRepository jpaServiceImageRepository;
    private final ServiceImageMapper serviceImageMapper;

    @Override
    public List<ServiceImage> findByServiceId(UUID serviceId) {
        List<ServiceImageJpaEntity> entities = jpaServiceImageRepository.findByServiceIdOrderBySortOrderAsc(serviceId);
        return serviceImageMapper.toDomainList(entities);
    }

    @Override
    public List<ServiceImage> saveAll(List<ServiceImage> images) {
        List<ServiceImageJpaEntity> entities = serviceImageMapper.toEntityList(images);
        List<ServiceImageJpaEntity> saved = jpaServiceImageRepository.saveAll(entities);
        return serviceImageMapper.toDomainList(saved);
    }

    @Override
    public void deleteByServiceId(UUID serviceId) {
        jpaServiceImageRepository.deleteByServiceId(serviceId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaServiceImageRepository.deleteById(id);
    }
}
```

#### `VendorPort.java` & `VendorAdapter.java`
Port: `com.danasea.backend.modules.service.domain.ports.VendorPort`
```java
package com.danasea.backend.modules.service.domain.ports;

import com.danasea.backend.modules.vendor.domain.models.Vendor;

import java.util.Optional;
import java.util.UUID;

public interface VendorPort {
    Optional<Vendor> findByUserId(UUID userId);
    Optional<Vendor> findById(UUID vendorId);
}
```

Adapter: `com.danasea.backend.modules.service.infrastructure.persistence.adapters.VendorAdapter`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import com.danasea.backend.modules.service.domain.ports.VendorPort;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VendorAdapter implements VendorPort {

    private final VendorInternalApi vendorInternalApi;

    @Override
    public Optional<Vendor> findByUserId(UUID userId) {
        return vendorInternalApi.findByUserId(userId);
    }

    @Override
    public Optional<Vendor> findById(UUID vendorId) {
        return vendorInternalApi.findById(vendorId);
    }
}
```

#### `AuditLogPort.java` & `AuditLogAdapter.java`
Port: `com.danasea.backend.modules.service.domain.ports.AuditLogPort`
```java
package com.danasea.backend.modules.service.domain.ports;

import java.util.UUID;

public interface AuditLogPort {
    void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata);
}
```

Adapter: `com.danasea.backend.modules.service.infrastructure.persistence.adapters.AuditLogAdapter`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.service.domain.ports.AuditLogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditLogAdapter implements AuditLogPort {

    private final AccountInternalApi accountInternalApi;

    @Override
    public void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata) {
        accountInternalApi.recordAuditLog(actorUserId, action, entityType, entityId, metadata);
    }
}
```

---

## 5. Verification Method
1. **Compilation Check**:
   Once classes are placed into their packages, execute:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   ./mvnw test-compile
   ```
2. **Unit Testing of Adapters & Mappers**:
   - `ServiceMapperTest`: verify `toDomain`, `toEntity`, and list transformations with full, partial, and null inputs.
   - `ServiceRepositoryAdapterTest`: mock `JpaServiceRepository` and `ServiceMapper`, verify `save`, `findById`, `findByVendorId`, `findByStatus`, `deleteById`.
   - `CategoryRepositoryAdapterTest`: mock `JpaCategoryRepository` and `CategoryMapper`, verify `findById` returning mapped domain or empty optional.
   - `ServiceImageRepositoryAdapterTest`: mock `JpaServiceImageRepository` and `ServiceImageMapper`, verify ordering in `findByServiceIdOrderBySortOrderAsc` and batch saving.
   - `VendorAdapterTest`: mock `VendorInternalApi`, test `findByUserId` and `findById`.
   - `AuditLogAdapterTest`: mock `AccountInternalApi`, test delegation of `recordAuditLog`.
3. **Invalidation Conditions**:
   - Renaming fields in `ServiceJpaEntity` or `BaseJpaEntity` without updating `ServiceMapper`.
   - Adding new required fields to `ServiceJpaEntity` without adding null defaults in `ServiceMapper.toEntity`.
   - Omitting `OrderByCreatedAtDesc` or `OrderBySortOrderAsc` in Spring Data query derivation causing unpredictable ordering.
