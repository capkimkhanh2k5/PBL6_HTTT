# Handoff Report: Milestone 1 Domain, Ports & Cross-Module Contracts

**Agent ID**: `m1_explorer_1`  
**Milestone**: M1 - Domain, Ports & Cross-Module Contracts  
**Target Module**: `com.danasea.backend.modules.service` (with cross-module integrations in `vendor` and `account`)  
**Date**: 2026-09-10  

---

## 1. Observation

### 1.1 Service Domain Models
- **`BaseDomainModel.java`** (`com/danasea/backend/shared/core/domain/models/BaseDomainModel.java:10-17`):
  - Annotated with `@Data`, `@SuperBuilder`, `@NoArgsConstructor`.
  - Defines `id` (UUID), `createdAt` (OffsetDateTime), `updatedAt` (OffsetDateTime).
- **`Service.java`** (`com/danasea/backend/modules/service/domain/models/Service.java:10-35`):
  - Currently annotated only with `@Data`, `@EqualsAndHashCode(callSuper = true)`. Lacks `@NoArgsConstructor`, `@AllArgsConstructor`, `@SuperBuilder`.
  - Fields: `vendorId`, `categoryId`, `name`, `nameEn`, `slug`, `description`, `descriptionEn`, `price`, `durationMinutes`, `capacityPerSlot`, `locationName`, `address`, `latitude`, `longitude`, `status`, `waiverContent`, `weatherSensitive`, `minWindKmh`, `maxWaveM`, `avgRating`, `ratingCount`, `viewCount`.
  - Does NOT currently have `rejectionReason` (needed for reject use case to store reason on service record) or domain validation/transition methods.
- **`Category.java`** (`com/danasea/backend/modules/service/domain/models/Category.java:9-18`):
  - Annotated only with `@Data`, `@EqualsAndHashCode(callSuper = true)`. Lacks `@NoArgsConstructor`, `@AllArgsConstructor`, `@SuperBuilder`.
  - Fields: `name`, `nameEn`, `slug`, `parentId`, `iconUrl`, `isActive`.
- **`ServiceImage.java`** (`com/danasea/backend/modules/service/domain/models/ServiceImage.java:9-15`):
  - Annotated only with `@Data`, `@EqualsAndHashCode(callSuper = true)`. Lacks `@NoArgsConstructor`, `@AllArgsConstructor`, `@SuperBuilder`.
  - Fields: `serviceId`, `url`, `sortOrder`.
- **`ServiceStatus.java`** (`com/danasea/backend/modules/service/domain/models/ServiceStatus.java:3-5`):
  - Enum constants: `DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `REJECTED`, `PAUSED`. Fully matches requirements.

### 1.2 JPA Entities & Repositories
- **`ServiceJpaEntity.java`** (`com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceJpaEntity.java`):
  - Table: `services`. Matches all fields in `Service.java`.
- **`CategoryJpaEntity.java`** (`com/danasea/backend/modules/service/infrastructure/persistence/entities/CategoryJpaEntity.java`):
  - Table: `categorys`. Matches all fields in `Category.java`.
- **`ServiceImageJpaEntity.java`** (`com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceImageJpaEntity.java`):
  - Table: `service_images`. Matches all fields in `ServiceImage.java`.
- **`JpaServiceRepository.java`**: Currently extends `JpaRepository<ServiceJpaEntity, UUID>` without custom query methods. Lacks `findByVendorId(UUID vendorId)` and `findByStatus(ServiceStatus status)`.
- **`JpaServiceImageRepository.java`**: Currently extends `JpaRepository<ServiceImageJpaEntity, UUID>`. Lacks `findByServiceId(UUID serviceId)`, `deleteByServiceId(UUID serviceId)`, `existsByServiceId(UUID serviceId)`.
- **`JpaCategoryRepository.java`**: Standard CRUD repository.

### 1.3 Missing Layers in `service` Module
- `com.danasea.backend.modules.service.domain.exceptions`: Currently 0 files.
- `com.danasea.backend.modules.service.domain.ports`: Currently 0 files.
- `com.danasea.backend.modules.service.infrastructure.persistence.adapters`: Currently 0 files.
- `com.danasea.backend.modules.service.infrastructure.persistence.mappers`: Currently 0 files.
- `com.danasea.backend.modules.service.application`: Currently 0 files.
- `com.danasea.backend.modules.service.presentation`: Currently 0 files.

### 1.4 Vendor Module State (`com.danasea.backend.modules.vendor`)
- Domain model: `Vendor.java` has `userId`, `verificationStatus`, `badgeTier`, etc.
- Entity: `VendorJpaEntity.java` has matching columns.
- Repository: `JpaVendorRepository.java` lacks `Optional<VendorJpaEntity> findByUserId(UUID userId)`.
- Application API: No `VendorInternalApi` or `VendorInternalService` or `VendorMapper` currently exists.

### 1.5 Account / AuditLog Module State (`com.danasea.backend.modules.account`)
- Domain model: `AuditLog.java` exists (`actorUserId`, `action`, `entityType`, `entityId`, `metadata`).
- Entity: `AuditLogJpaEntity.java` exists (`audit_logs` table).
- Repository: `JpaAuditLogRepository.java` exists.
- Application API: `AccountInternalApi.java` and `AccountInternalService.java` exist, but DO NOT yet have an audit logging method (`recordAuditLog(...)`).

### 1.6 Build Environment
- Java SDK: Eclipse Adoptium OpenJDK 21.0.10 (`/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`).
- Build tool: Maven wrapper `./mvnw`.
- Verification command: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home && export PATH=$JAVA_HOME/bin:$PATH && ./mvnw test` runs 30 tests with 0 failures.

---

## 2. Logic Chain

### 2.1 Domain Models Enhancement
- **Observation**: `BaseDomainModel` uses `@SuperBuilder` and `@NoArgsConstructor`. `Service`, `Category`, and `ServiceImage` extend `BaseDomainModel` but only declare `@Data` and `@EqualsAndHashCode(callSuper = true)`.
- **Reasoning**: Without `@SuperBuilder`, `@NoArgsConstructor`, and `@AllArgsConstructor`, building domain objects in use cases, unit tests, and mappers is tedious and error-prone. In addition, `Service` requires state transition logic (R1, R2, R3).
- **Inference**: Adding `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor` and domain-driven state transition methods directly on `Service` enforces business invariants at compile time and runtime.

### 2.2 Domain Exceptions Design
- **Observation**: Requirements R1, R2, R3 specify explicit business rule validations:
  - Non-existent category -> 404 (`CategoryNotFoundException`)
  - Inactive category -> 400 (`CategoryInactiveException`)
  - Missing weather requirements when `weather_sensitive=true` -> 400 (`WeatherRequirementsMissingException`)
  - Submitting without images -> 400 (`ServiceImagesRequiredException`)
  - Invalid state transition (e.g. deleting non-DRAFT, pausing non-PUBLISHED, approving non-PENDING_REVIEW) -> 400 (`InvalidServiceStateException`)
  - Vendor not approved (e.g. status is PENDING/REJECTED) -> 400/403 (`VendorNotApprovedException`)
  - Accessing/modifying service owned by another vendor -> 403 (`UnauthorizedServiceAccessException`)
  - Service not found -> 404 (`ServiceNotFoundException`)
- **Reasoning**: All domain exceptions should extend a common root `ServiceDomainException extends RuntimeException`. This adheres to Clean Architecture rules (domain remains pure Java) and facilitates centralized mapping in `ServiceExceptionHandler` (Presentation layer) to `ErrorResponse(code, message)`.

### 2.3 Domain Ports Design
- **Observation**: The Service domain needs persistence and cross-module interactions:
  - `ServiceRepositoryPort`: CRUD + query by vendor and status.
  - `CategoryRepositoryPort`: Check category existence and active status.
  - `ServiceImageRepositoryPort`: Check images count on submit, query images, batch save images.
  - `VendorPort`: Verify vendor status by user ID / vendor ID without coupling domain to JPA repositories of another module.
  - `AuditLogPort`: Record audit logs on admin approval / rejection without coupling domain to the `account` module persistence layer.
- **Reasoning**: Hexagonal Ports & Adapters isolate the core domain. Repositories in `modules.service.domain.ports` will be implemented by adapters in `modules.service.infrastructure.persistence.adapters`.

### 2.4 Cross-Module Integration Contracts
- **Observation**:
  - `VendorPort` requires finding vendor by user ID (from JWT principal) and checking approval.
  - `AuditLogPort` requires logging admin actions (`SERVICE_APPROVED`, `SERVICE_REJECTED`).
- **Reasoning**: Following the Module Dependency Rule (`Clean_Architecture_Rules.md` §11), `service` must interact with `vendor` and `account` via public application contracts (`VendorInternalApi` and `AccountInternalApi`), wrapped by domain ports (`VendorPort` and `AuditLogPort`).
  - Add `findByUserId` to `JpaVendorRepository`.
  - Create `VendorInternalApi` and `VendorInternalService` in `modules/vendor`.
  - Add `recordAuditLog` to `AccountInternalApi` and implement in `AccountInternalService`.

---

## 3. Caveats

1. **Database Schema & `rejection_reason` column**:
   - `ServiceJpaEntity` does not have a `rejection_reason` column yet.
   - `ORIGINAL_REQUEST.md` requires: `PATCH /api/admin/services/{id}/reject (Từ chối: -> REJECTED, kèm reason, ghi audit log)`.
   - We recommend adding `rejectionReason` (String) to both `Service.java` and `ServiceJpaEntity.java` so vendors can inspect why their service was rejected when viewing service details (`GET /api/vendor/services/{id}`). Since `hibernate.ddl-auto: update` is active, Hibernate will automatically add the column.
2. **Vendor Approval Check on Creation**:
   - When a Vendor calls `POST /api/vendor/services`, the authenticated user provides a JWT containing `userId`.
   - The use case must resolve `userId` -> `Vendor` via `vendorPort.findByUserId(userId)`.
   - If vendor is not found OR `vendor.getVerificationStatus() != VerificationStatus.APPROVED`, it must throw `VendorNotApprovedException`.
3. **Cross-Module Boundaries**:
   - Do NOT inject `JpaVendorRepository` or `JpaAuditLogRepository` directly into `service` module use cases or adapters. Use `VendorInternalApi` and `AccountInternalApi`.

---

## 4. Conclusion & Technical Specifications

### 4.1 Domain Models Specification

#### `Service.java` (`com.danasea.backend.modules.service.domain.models`)
```java
package com.danasea.backend.modules.service.domain.models;

import java.math.BigDecimal;
import java.util.UUID;
import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import com.danasea.backend.modules.service.domain.exceptions.InvalidServiceStateException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceImagesRequiredException;
import com.danasea.backend.modules.service.domain.exceptions.UnauthorizedServiceAccessException;
import com.danasea.backend.modules.service.domain.exceptions.WeatherRequirementsMissingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Service extends BaseDomainModel {
    private UUID vendorId;
    private UUID categoryId;
    private String name;
    private String nameEn;
    private String slug;
    private String description;
    private String descriptionEn;
    private BigDecimal price;
    private Integer durationMinutes;
    private Integer capacityPerSlot;
    private String locationName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private ServiceStatus status;
    private String rejectionReason;
    private String waiverContent;
    private Boolean weatherSensitive;
    private BigDecimal minWindKmh;
    private BigDecimal maxWaveM;
    private BigDecimal avgRating;
    private Integer ratingCount;
    private Integer viewCount;

    // Domain Invariant & Business Helper Methods
    public boolean isDraft() {
        return ServiceStatus.DRAFT.equals(this.status);
    }

    public boolean isPendingReview() {
        return ServiceStatus.PENDING_REVIEW.equals(this.status);
    }

    public boolean isPublished() {
        return ServiceStatus.PUBLISHED.equals(this.status);
    }

    public boolean isPaused() {
        return ServiceStatus.PAUSED.equals(this.status);
    }

    public boolean isRejected() {
        return ServiceStatus.REJECTED.equals(this.status);
    }

    public boolean isOwnedBy(UUID vendorId) {
        return this.vendorId != null && this.vendorId.equals(vendorId);
    }

    public void validateOwnership(UUID vendorId) {
        if (!isOwnedBy(vendorId)) {
            throw new UnauthorizedServiceAccessException(this.getId(), vendorId);
        }
    }

    public void validateWeatherRequirements() {
        if (Boolean.TRUE.equals(this.weatherSensitive)) {
            if (this.minWindKmh == null || this.maxWaveM == null) {
                throw new WeatherRequirementsMissingException();
            }
        }
    }

    public void submitForReview(boolean hasImages) {
        if (!isDraft() && !isRejected()) {
            throw new InvalidServiceStateException("Cannot submit service in status " + this.status + " for review. Must be DRAFT or REJECTED.");
        }
        if (!hasImages) {
            throw new ServiceImagesRequiredException();
        }
        this.status = ServiceStatus.PENDING_REVIEW;
    }

    public void approve() {
        if (!isPendingReview()) {
            throw new InvalidServiceStateException("Cannot approve service in status " + this.status + ". Must be PENDING_REVIEW.");
        }
        this.status = ServiceStatus.PUBLISHED;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        if (!isPendingReview()) {
            throw new InvalidServiceStateException("Cannot reject service in status " + this.status + ". Must be PENDING_REVIEW.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        this.status = ServiceStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void pause() {
        if (!isPublished()) {
            throw new InvalidServiceStateException("Cannot pause service in status " + this.status + ". Must be PUBLISHED.");
        }
        this.status = ServiceStatus.PAUSED;
    }

    public void resume() {
        if (!isPaused()) {
            throw new InvalidServiceStateException("Cannot resume service in status " + this.status + ". Must be PAUSED.");
        }
        this.status = ServiceStatus.PUBLISHED;
    }

    public void validateDeletable() {
        if (!isDraft()) {
            throw new InvalidServiceStateException("Cannot delete service in status " + this.status + ". Only DRAFT services can be deleted.");
        }
    }

    public void transitionOnUpdate() {
        if (isPublished()) {
            this.status = ServiceStatus.PENDING_REVIEW;
        }
    }
}
```

#### `Category.java` (`com.danasea.backend.modules.service.domain.models`)
```java
package com.danasea.backend.modules.service.domain.models;

import java.util.UUID;
import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Category extends BaseDomainModel {
    private String name;
    private String nameEn;
    private String slug;
    private UUID parentId;
    private String iconUrl;
    private Boolean isActive;

    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }
}
```

#### `ServiceImage.java` (`com.danasea.backend.modules.service.domain.models`)
```java
package com.danasea.backend.modules.service.domain.models;

import java.util.UUID;
import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ServiceImage extends BaseDomainModel {
    private UUID serviceId;
    private String url;
    private Short sortOrder;
}
```

---

### 4.2 Domain Exceptions Hierarchy (`com.danasea.backend.modules.service.domain.exceptions`)

| Exception | Extends | HTTP Status | Error Code | Constructors |
|---|---|---|---|---|
| `ServiceDomainException` | `RuntimeException` | 500 (Base) | `SERVICE_DOMAIN_ERROR` | `(String message)`, `(String message, Throwable cause)` |
| `ServiceNotFoundException` | `ServiceDomainException` | 404 | `SERVICE_NOT_FOUND` | `(UUID serviceId)`, `(String message)` |
| `CategoryNotFoundException` | `ServiceDomainException` | 404 | `CATEGORY_NOT_FOUND` | `(UUID categoryId)`, `(String message)` |
| `CategoryInactiveException` | `ServiceDomainException` | 400 | `CATEGORY_INACTIVE` | `(UUID categoryId)`, `(String message)` |
| `WeatherRequirementsMissingException` | `ServiceDomainException` | 400 | `WEATHER_REQUIREMENTS_MISSING` | `()`, `(String message)` |
| `ServiceImagesRequiredException` | `ServiceDomainException` | 400 | `SERVICE_IMAGES_REQUIRED` | `()`, `(String message)` |
| `InvalidServiceStateException` | `ServiceDomainException` | 400 | `INVALID_SERVICE_STATE` | `(String message)`, `(ServiceStatus status, String action)` |
| `VendorNotApprovedException` | `ServiceDomainException` | 400 (or 403) | `VENDOR_NOT_APPROVED` | `(UUID vendorId)`, `(String message)` |
| `UnauthorizedServiceAccessException` | `ServiceDomainException` | 403 | `UNAUTHORIZED_SERVICE_ACCESS` | `(String message)`, `(UUID serviceId, UUID vendorId)` |

---

### 4.3 Domain Ports Specification (`com.danasea.backend.modules.service.domain.ports`)

#### 1. `ServiceRepositoryPort.java`
```java
package com.danasea.backend.modules.service.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;

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

#### 2. `CategoryRepositoryPort.java`
```java
package com.danasea.backend.modules.service.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.danasea.backend.modules.service.domain.models.Category;

public interface CategoryRepositoryPort {
    Optional<Category> findById(UUID id);
    boolean existsById(UUID id);
    List<Category> findAll();
}
```

#### 3. `ServiceImageRepositoryPort.java`
```java
package com.danasea.backend.modules.service.domain.ports;

import java.util.List;
import java.util.UUID;
import com.danasea.backend.modules.service.domain.models.ServiceImage;

public interface ServiceImageRepositoryPort {
    List<ServiceImage> findByServiceId(UUID serviceId);
    List<ServiceImage> saveAll(List<ServiceImage> images);
    void deleteByServiceId(UUID serviceId);
    void deleteById(UUID id);
    boolean existsByServiceId(UUID serviceId);
}
```

#### 4. `VendorPort.java`
```java
package com.danasea.backend.modules.service.domain.ports;

import java.util.Optional;
import java.util.UUID;
import com.danasea.backend.modules.vendor.domain.models.Vendor;

public interface VendorPort {
    Optional<Vendor> findByUserId(UUID userId);
    Optional<Vendor> findById(UUID vendorId);
    boolean isVendorApproved(UUID vendorId);
}
```

#### 5. `AuditLogPort.java`
```java
package com.danasea.backend.modules.service.domain.ports;

import java.util.UUID;

public interface AuditLogPort {
    void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata);
}
```

---

### 4.4 Cross-Module Integration Contracts

#### In `com.danasea.backend.modules.vendor`:
1. **`JpaVendorRepository.java`**:
   ```java
   Optional<VendorJpaEntity> findByUserId(UUID userId);
   ```
2. **`VendorInternalApi.java`** (`com.danasea.backend.modules.vendor.application.api`):
   ```java
   package com.danasea.backend.modules.vendor.application.api;

   import java.util.Optional;
   import java.util.UUID;
   import com.danasea.backend.modules.vendor.domain.models.Vendor;

   public interface VendorInternalApi {
       Optional<Vendor> findByUserId(UUID userId);
       Optional<Vendor> findById(UUID vendorId);
       boolean isVendorApproved(UUID vendorId);
   }
   ```
3. **`VendorInternalService.java`** (`com.danasea.backend.modules.vendor.application.service`):
   Implements `VendorInternalApi` using `JpaVendorRepository` and `VendorMapper`.
4. **`VendorMapper.java`** (`com.danasea.backend.modules.vendor.infrastructure.mapper`):
   Maps between `VendorJpaEntity` and `Vendor` domain model.

#### In `com.danasea.backend.modules.account`:
1. **`AccountInternalApi.java`**:
   Add method:
   ```java
   void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata);
   ```
2. **`AccountInternalService.java`**:
   Implement `recordAuditLog(...)` by saving `AuditLogJpaEntity` via existing `JpaAuditLogRepository`.

#### In `com.danasea.backend.modules.service.infrastructure.persistence.adapters`:
1. **`VendorAdapter.java`**:
   Implements `VendorPort`, delegates to `VendorInternalApi`.
2. **`AuditLogAdapter.java`**:
   Implements `AuditLogPort`, delegates to `AccountInternalApi`.

---

## 5. Verification Method

To verify the implementation independently:

1. **Compilation Check**:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   cd backend
   ./mvnw clean test-compile -DskipTests
   ```
   *Expected outcome*: `BUILD SUCCESS` with zero compilation errors.

2. **Test Suite Execution**:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   cd backend
   ./mvnw test
   ```
   *Expected outcome*: All tests pass (0 failures, 0 errors).

3. **Layout & Clean Architecture Compliance**:
   Inspect that:
   - `modules/service/domain/` has NO imports from `jakarta.persistence.*`, `org.springframework.*`, or presentation packages.
   - `VendorPort` and `AuditLogPort` are pure Java interfaces located in `modules/service/domain/ports/`.
   - All 8 domain exceptions extend `ServiceDomainException` in `modules/service/domain/exceptions/`.
