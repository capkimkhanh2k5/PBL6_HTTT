# Báo Cáo Handoff: Khảo Sát & Thiết Kế Cross-Module Contracts (Vendor, Account & Service)

**Agent ID**: `m1_explorer_2`  
**Milestone**: Milestone 1: Domain, Ports & Cross-Module Contracts  
**Phạm vi khảo sát**: Cross-module dependencies giữa module `service` với module `vendor` và `account`, tuân thủ nguyên tắc Clean Architecture.  
**Ngày thực hiện**: 2026-09-10  

---

## 1. Observation (Quan sát thực tế)

### 1.1 Khảo sát Module `vendor`
- **Domain Model**: `Vendor.java` (`com.danasea.backend.modules.vendor.domain.models.Vendor`):
  - Kế thừa `BaseDomainModel` (`id`, `createdAt`, `updatedAt`).
  - Có thuộc tính `private UUID userId;` (ID tài khoản liên kết với vendor) và `private VerificationStatus verificationStatus;` (`PENDING`, `APPROVED`, `REJECTED`).
- **JPA Entity**: `VendorJpaEntity.java` (`com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity`):
  - Mapping tới bảng `vendors`.
  - Có cột `private UUID userId;` và enum `VerificationStatus verificationStatus;`.
- **JPA Repository**: `JpaVendorRepository.java` (`com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository`):
  - Hiện tại chỉ kế thừa `JpaRepository<VendorJpaEntity, UUID>`, hoàn toàn **chưa có** method tìm theo `userId` (`findByUserId`).
- **Application API / Service Layer**:
  - Thư mục `modules/vendor` hiện tại chỉ có `domain/` và `infrastructure/`.
  - Hoàn toàn **chưa có** package `application/api/`, chưa có `VendorInternalApi`, chưa có `VendorInternalService`, và chưa có `VendorMapper`.

### 1.2 Khảo sát Module `account`
- **Domain Model**: `AuditLog.java` (`com.danasea.backend.modules.account.domain.models.AuditLog`):
  - Kế thừa `BaseDomainModel`.
  - Các thuộc tính: `actorUserId` (UUID), `action` (String), `entityType` (String), `entityId` (UUID), `metadata` (String).
- **JPA Entity**: `AuditLogJpaEntity.java` (`com.danasea.backend.modules.account.infrastructure.persistence.entities.AuditLogJpaEntity`):
  - Mapping tới bảng `audit_logs`.
  - Các cột: `actorUserId`, `action`, `entityType`, `entityId`, `metadata`.
- **JPA Repository**: `JpaAuditLogRepository.java` (`com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaAuditLogRepository`):
  - Kế thừa `JpaRepository<AuditLogJpaEntity, UUID>`.
- **Application API / Service Layer**:
  - `AccountInternalApi.java` (`com.danasea.backend.modules.account.application.api.AccountInternalApi`): Hiện tại chỉ có các method quản lý user và refresh token (`findUserByEmail`, `findUserById`, `existsByEmail`, `saveUser`, `saveRefreshToken`, `findRefreshTokenByHash`, `revokeRefreshTokenFamily`).
  - `AccountInternalService.java` (`com.danasea.backend.modules.account.application.service.AccountInternalService`): Đang inject `JpaUserRepository`, `JpaRefreshTokenRepository`, `UserMapper`, `RefreshTokenMapper`. Chưa inject `JpaAuditLogRepository` và chưa có method ghi audit log.

### 1.3 Khảo sát Kiến Trúc & Quy Tắc Clean Architecture (`backend/docs/Clean_Architecture_Rules.md`)
- **Nguyên tắc Dependency (Rule 1)**: Dependencies phải hướng vào trong (`Presentation → Application → Domain ← Infrastructure`). Inner layer không được phụ thuộc outer layer.
- **Nguyên tắc Abstraction / Port (Rule 6)**: Khi một inner layer cần chức năng bên ngoài, inner layer định nghĩa abstraction (interface/port). Outer layer sẽ implement abstraction đó.
- **Nguyên tắc Module Dependency (Rule 11)**:
  - `Module A → Public abstraction/contract → Module B` (Khuyến khích tuyệt đối).
  - Không được để `Module A → Internal implementation of Module B`.
- **Tiền lệ thiết kế trong Codebase**:
  - `security.authorization` phụ thuộc vào `account` thông qua `AccountInternalApi` (đặt tại `com.danasea.backend.modules.account.application.api`).
  - Lớp `AuthorizationAdapter` (thuộc Infrastructure của module authorization) inject `AccountInternalApi` để implement `AuthorizationPort` (của module authorization).
  - Đây là mẫu chuẩn đã hoạt động ổn định và nhất quán trong toàn bộ hệ thống.

---

## 2. Logic Chain (Chuỗi lập luận kỹ thuật)

### 2.1 Thiết kế cho Module `vendor`
1. **Truy vấn Vendor theo `userId`**:
   - Khi một Vendor đăng nhập, Principal trong SecurityContext cung cấp Email hoặc UserId. Khi Vendor gọi `POST /api/vendor/services`, hệ thống cần định danh Vendor từ UserId của User đang đăng nhập và kiểm tra trạng thái phê duyệt (`verificationStatus == VerificationStatus.APPROVED`).
   - `JpaVendorRepository` cần thêm method:
     ```java
     Optional<VendorJpaEntity> findByUserId(UUID userId);
     ```
   - Spring Data JPA tự động sinh câu truy vấn `SELECT v FROM VendorJpaEntity v WHERE v.userId = :userId`.

2. **Công khai Contract `VendorInternalApi`**:
   - Theo Rule 11 (Module Dependency Rule), module `service` không được phép gọi trực tiếp `JpaVendorRepository` hay `VendorJpaEntity` của module `vendor`.
   - Module `vendor` cần cung cấp một Contract công khai tại Application layer:
     `com.danasea.backend.modules.vendor.application.api.VendorInternalApi`
   - Các method yêu cầu:
     - `Optional<Vendor> findByUserId(UUID userId);` (Phục vụ Vendor endpoints để xác định chủ sở hữu và kiểm tra quyền APPROVED).
     - `Optional<Vendor> findById(UUID vendorId);` (Phục vụ truy vấn thông tin chi tiết vendor từ service).
   - Domain Model `Vendor` được trả về (không lộ JPA Entity ra ngoài module).

3. **Hiện thực hóa Contract (`VendorMapper` & `VendorInternalService`)**:
   - Để chuyển đổi giữa `VendorJpaEntity` và domain `Vendor`, tạo `VendorMapper` trong `com.danasea.backend.modules.vendor.infrastructure.mapper`.
   - Tạo `VendorInternalService` trong `com.danasea.backend.modules.vendor.application.service`, đánh dấu `@Service`, implement `VendorInternalApi`, inject `JpaVendorRepository` và `VendorMapper`.

### 2.2 Thiết kế cho Module `account` (AuditLog)
1. **Yêu cầu nghiệp vụ ghi Audit Log (R2, PROJECT.md #14)**:
   - Khi Admin duyệt dịch vụ (`PATCH /api/admin/services/{id}/approve`), cần ghi audit log với `action = "SERVICE_APPROVED"`.
   - Khi Admin từ chối dịch vụ (`PATCH /api/admin/services/{id}/reject`), cần ghi audit log với `action = "SERVICE_REJECTED"` kèm lý do từ chối.
   - Bảng `audit_logs` có các cột: `actor_user_id`, `action`, `entity_type`, `entity_id`, `metadata`.

2. **Mở rộng `AccountInternalApi`**:
   - Bổ sung method tiện ích chuẩn vào `AccountInternalApi`:
     ```java
     void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata);
     ```
   - Việc nhận các tham số nguyên thủy/UUID thay vì yêu cầu caller tạo `AuditLog` domain object giúp caller hoàn toàn không bị rườm rà về việc khởi tạo ID, timestamp, hay import model của module khác.

3. **Hiện thực hóa tại `AccountInternalService`**:
   - Inject `JpaAuditLogRepository auditLogRepository` vào `AccountInternalService`.
   - Method `recordAuditLog`: Tạo `AuditLogJpaEntity`, gán các giá trị tham số và gọi `auditLogRepository.save(entity)`.

### 2.3 Mô hình Tách Rời Hai Tầng (Two-Tier Decoupling) cho Module `service`
Để module `service` tuân thủ tuyệt đối Rule 1 (Dependency Rule) và Rule 6 (Interface / Abstraction Rule):
1. **Domain Ports trong `modules.service.domain.ports`**:
   - `VendorPort`: Cung cấp `Optional<Vendor> findByUserId(UUID userId)` và `Optional<Vendor> findById(UUID vendorId)`.
   - `AuditLogPort`: Cung cấp `void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata)`.
   - Các Use Case trong `service.application.usecases` (như `CreateServiceUseCase`, `ApproveServiceUseCase`, `RejectServiceUseCase`) **chỉ phụ thuộc vào 2 Domain Ports này**.
2. **Infrastructure Adapters trong `modules.service.infrastructure.persistence.adapters`**:
   - `VendorAdapter implements VendorPort`: Đánh dấu `@Component`, inject `VendorInternalApi`.
   - `AuditLogAdapter implements AuditLogPort`: Đánh dấu `@Component`, inject `AccountInternalApi`.
3. **Lợi ích kiến trúc**:
   - Tầng Domain và Application của `service` hoàn toàn độc lập, có thể viết Unit Test cực nhanh bằng cách Mock `VendorPort` và `AuditLogPort`.
   - Tầng Infrastructure đóng vai trò làm cầu nối (Adapter) gọi sang Public Contract (`InternalApi`) của các module khác.
   - Không vi phạm quy tắc đóng gói, không tạo vòng phụ thuộc (cyclic dependencies).

---

## 3. Caveats (Lưu ý & Điểm đặc biệt)

1. **Vendor Approval Check Logic**:
   - Khi Vendor tạo dịch vụ, ngoài việc tìm thấy Vendor theo `userId`, UseCase bắt buộc phải kiểm tra `vendor.getVerificationStatus() == VerificationStatus.APPROVED`. Nếu trạng thái là `PENDING` hoặc `REJECTED`, phải ném `VendorNotApprovedException` (map tới mã lỗi HTTP 400 hoặc 403 theo R3).
2. **Rejection Reason trong Audit Log**:
   - Khi Admin Reject, `reason` là bắt buộc. Metadata truyền vào `recordAuditLog` có thể là raw string `reason` hoặc JSON `{"reason": "..."}`. Nên thống nhất định dạng chuỗi đơn giản hoặc JSON payload hợp lệ.
3. **Spring Component Scanning**:
   - Gốc ứng dụng là `com.danasea.backend.BackendApplication`. Tất cả các module đều nằm dưới `com.danasea.backend.modules.*`, do đó Spring Boot tự động scan và inject các `@Service`, `@Component`, `@Repository` mà không cần cấu hình scan bổ sung.

---

## 4. Conclusion & Chi Tiết Triển Khai (Chi Tiết Code & Wiring)

### 4.1 Module `vendor`

#### (A) `JpaVendorRepository.java`
- Đường dẫn: `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/repositories/JpaVendorRepository.java`
```java
package com.danasea.backend.modules.vendor.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaVendorRepository extends JpaRepository<VendorJpaEntity, UUID> {
    Optional<VendorJpaEntity> findByUserId(UUID userId);
}
```

#### (B) `VendorMapper.java`
- Đường dẫn: `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/mapper/VendorMapper.java`
```java
package com.danasea.backend.modules.vendor.infrastructure.mapper;

import org.springframework.stereotype.Component;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;

@Component
public class VendorMapper {

    public Vendor toDomain(VendorJpaEntity entity) {
        if (entity == null) return null;
        Vendor domain = new Vendor();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setUserId(entity.getUserId());
        domain.setBusinessName(entity.getBusinessName());
        domain.setTaxCode(entity.getTaxCode());
        domain.setAddress(entity.getAddress());
        domain.setBankAccountNumber(entity.getBankAccountNumber());
        domain.setBankName(entity.getBankName());
        domain.setBankAccountHolder(entity.getBankAccountHolder());
        domain.setVerificationStatus(entity.getVerificationStatus());
        domain.setVerifiedBy(entity.getVerifiedBy());
        domain.setVerifiedAt(entity.getVerifiedAt());
        domain.setRatingAvg(entity.getRatingAvg());
        domain.setRatingCount(entity.getRatingCount());
        domain.setBadgeTier(entity.getBadgeTier());
        return domain;
    }

    public VendorJpaEntity toEntity(Vendor domain) {
        if (domain == null) return null;
        VendorJpaEntity entity = new VendorJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setBusinessName(domain.getBusinessName());
        entity.setTaxCode(domain.getTaxCode());
        entity.setAddress(domain.getAddress());
        entity.setBankAccountNumber(domain.getBankAccountNumber());
        entity.setBankName(domain.getBankName());
        entity.setBankAccountHolder(domain.getBankAccountHolder());
        entity.setVerificationStatus(domain.getVerificationStatus());
        entity.setVerifiedBy(domain.getVerifiedBy());
        entity.setVerifiedAt(domain.getVerifiedAt());
        entity.setRatingAvg(domain.getRatingAvg());
        entity.setRatingCount(domain.getRatingCount());
        entity.setBadgeTier(domain.getBadgeTier());
        return entity;
    }
}
```

#### (C) `VendorInternalApi.java`
- Đường dẫn: `backend/src/main/java/com/danasea/backend/modules/vendor/application/api/VendorInternalApi.java`
```java
package com.danasea.backend.modules.vendor.application.api;

import java.util.Optional;
import java.util.UUID;
import com.danasea.backend.modules.vendor.domain.models.Vendor;

public interface VendorInternalApi {
    Optional<Vendor> findByUserId(UUID userId);
    Optional<Vendor> findById(UUID vendorId);
}
```

#### (D) `VendorInternalService.java`
- Đường dẫn: `backend/src/main/java/com/danasea/backend/modules/vendor/application/service/VendorInternalService.java`
```java
package com.danasea.backend.modules.vendor.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.infrastructure.mapper.VendorMapper;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository;

@Service
@RequiredArgsConstructor
public class VendorInternalService implements VendorInternalApi {

    private final JpaVendorRepository vendorRepository;
    private final VendorMapper vendorMapper;

    @Override
    public Optional<Vendor> findByUserId(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .map(vendorMapper::toDomain);
    }

    @Override
    public Optional<Vendor> findById(UUID vendorId) {
        return vendorRepository.findById(vendorId)
                .map(vendorMapper::toDomain);
    }
}
```

---

### 4.2 Module `account`

#### (A) `AccountInternalApi.java` (Thêm method `recordAuditLog`)
- Đường dẫn: `backend/src/main/java/com/danasea/backend/modules/account/application/api/AccountInternalApi.java`
```java
package com.danasea.backend.modules.account.application.api;

import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.account.domain.models.RefreshToken;
import com.danasea.backend.modules.account.domain.models.User;

public interface AccountInternalApi {
    Optional<User> findUserByEmail(String email);

    Optional<User> findUserById(UUID id);

    boolean existsByEmail(String email);

    User saveUser(User user);

    RefreshToken saveRefreshToken(RefreshToken token);

    Optional<RefreshToken> findRefreshTokenByHash(String tokenHash);

    void revokeRefreshTokenFamily(UUID familyId);

    void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata);
}
```

#### (B) `AuditLogMapper.java`
- Đường dẫn: `backend/src/main/java/com/danasea/backend/modules/account/infrastructure/mapper/AuditLogMapper.java`
```java
package com.danasea.backend.modules.account.infrastructure.mapper;

import org.springframework.stereotype.Component;
import com.danasea.backend.modules.account.domain.models.AuditLog;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.AuditLogJpaEntity;

@Component
public class AuditLogMapper {

    public AuditLog toDomain(AuditLogJpaEntity entity) {
        if (entity == null) return null;
        AuditLog domain = new AuditLog();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setActorUserId(entity.getActorUserId());
        domain.setAction(entity.getAction());
        domain.setEntityType(entity.getEntityType());
        domain.setEntityId(entity.getEntityId());
        domain.setMetadata(entity.getMetadata());
        return domain;
    }

    public AuditLogJpaEntity toEntity(AuditLog domain) {
        if (domain == null) return null;
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setId(domain.getId());
        entity.setActorUserId(domain.getActorUserId());
        entity.setAction(domain.getAction());
        entity.setEntityType(domain.getEntityType());
        entity.setEntityId(domain.getEntityId());
        entity.setMetadata(domain.getMetadata());
        return entity;
    }
}
```

#### (C) `AccountInternalService.java` (Cập nhật ghi audit log)
- Đường dẫn: `backend/src/main/java/com/danasea/backend/modules/account/application/service/AccountInternalService.java`
- Bổ sung dependency: `private final JpaAuditLogRepository auditLogRepository;`
- Bổ sung triển khai method:
```java
    @Override
    public void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setActorUserId(actorUserId);
        entity.setAction(action);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setMetadata(metadata);
        auditLogRepository.save(entity);
    }
```

---

### 4.3 Module `service`: Domain Ports, Infrastructure Adapters & Wiring

#### (A) `VendorPort.java`
- Đường dẫn: `backend/src/main/java/com/danasea/backend/modules/service/domain/ports/VendorPort.java`
```java
package com.danasea.backend.modules.service.domain.ports;

import java.util.Optional;
import java.util.UUID;
import com.danasea.backend.modules.vendor.domain.models.Vendor;

public interface VendorPort {
    Optional<Vendor> findByUserId(UUID userId);
    Optional<Vendor> findById(UUID vendorId);
}
```

#### (B) `AuditLogPort.java`
- Đường dẫn: `backend/src/main/java/com/danasea/backend/modules/service/domain/ports/AuditLogPort.java`
```java
package com.danasea.backend.modules.service.domain.ports;

import java.util.UUID;

public interface AuditLogPort {
    void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata);
}
```

#### (C) `VendorAdapter.java`
- Đường dẫn: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/adapters/VendorAdapter.java`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import com.danasea.backend.modules.service.domain.ports.VendorPort;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;

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

#### (D) `AuditLogAdapter.java`
- Đường dẫn: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/adapters/AuditLogAdapter.java`
```java
package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.UUID;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.service.domain.ports.AuditLogPort;

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

#### (E) Spring Bean Wiring: `ServiceBeans.java`
- Đường dẫn: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/config/ServiceBeans.java`
```java
package com.danasea.backend.modules.service.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasea.backend.modules.service.domain.ports.AuditLogPort;
import com.danasea.backend.modules.service.domain.ports.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.VendorPort;
import com.danasea.backend.modules.service.application.usecases.CreateServiceUseCase;
import com.danasea.backend.modules.service.application.usecases.ApproveServiceUseCase;
import com.danasea.backend.modules.service.application.usecases.RejectServiceUseCase;
import com.danasea.backend.modules.service.application.usecases.SubmitServiceForReviewUseCase;
import com.danasea.backend.modules.service.application.usecases.UpdateServiceUseCase;
import com.danasea.backend.modules.service.application.usecases.DeleteServiceUseCase;
import com.danasea.backend.modules.service.application.usecases.PauseServiceUseCase;
import com.danasea.backend.modules.service.application.usecases.ResumeServiceUseCase;
import com.danasea.backend.modules.service.application.usecases.GetVendorServicesUseCase;
import com.danasea.backend.modules.service.application.usecases.GetAdminServicesUseCase;

@Configuration
public class ServiceBeans {

    @Bean
    public CreateServiceUseCase createServiceUseCase(
            ServiceRepositoryPort serviceRepositoryPort,
            CategoryRepositoryPort categoryRepositoryPort,
            VendorPort vendorPort) {
        return new CreateServiceUseCase(serviceRepositoryPort, categoryRepositoryPort, vendorPort);
    }

    @Bean
    public SubmitServiceForReviewUseCase submitServiceForReviewUseCase(
            ServiceRepositoryPort serviceRepositoryPort,
            ServiceImageRepositoryPort serviceImageRepositoryPort,
            VendorPort vendorPort) {
        return new SubmitServiceForReviewUseCase(serviceRepositoryPort, serviceImageRepositoryPort, vendorPort);
    }

    @Bean
    public UpdateServiceUseCase updateServiceUseCase(
            ServiceRepositoryPort serviceRepositoryPort,
            CategoryRepositoryPort categoryRepositoryPort,
            VendorPort vendorPort) {
        return new UpdateServiceUseCase(serviceRepositoryPort, categoryRepositoryPort, vendorPort);
    }

    @Bean
    public ApproveServiceUseCase approveServiceUseCase(
            ServiceRepositoryPort serviceRepositoryPort,
            AuditLogPort auditLogPort) {
        return new ApproveServiceUseCase(serviceRepositoryPort, auditLogPort);
    }

    @Bean
    public RejectServiceUseCase rejectServiceUseCase(
            ServiceRepositoryPort serviceRepositoryPort,
            AuditLogPort auditLogPort) {
        return new RejectServiceUseCase(serviceRepositoryPort, auditLogPort);
    }

    @Bean
    public DeleteServiceUseCase deleteServiceUseCase(
            ServiceRepositoryPort serviceRepositoryPort,
            VendorPort vendorPort) {
        return new DeleteServiceUseCase(serviceRepositoryPort, vendorPort);
    }

    @Bean
    public PauseServiceUseCase pauseServiceUseCase(
            ServiceRepositoryPort serviceRepositoryPort,
            VendorPort vendorPort) {
        return new PauseServiceUseCase(serviceRepositoryPort, vendorPort);
    }

    @Bean
    public ResumeServiceUseCase resumeServiceUseCase(
            ServiceRepositoryPort serviceRepositoryPort,
            VendorPort vendorPort) {
        return new ResumeServiceUseCase(serviceRepositoryPort, vendorPort);
    }

    @Bean
    public GetVendorServicesUseCase getVendorServicesUseCase(
            ServiceRepositoryPort serviceRepositoryPort,
            VendorPort vendorPort) {
        return new GetVendorServicesUseCase(serviceRepositoryPort, vendorPort);
    }

    @Bean
    public GetAdminServicesUseCase getAdminServicesUseCase(
            ServiceRepositoryPort serviceRepositoryPort) {
        return new GetAdminServicesUseCase(serviceRepositoryPort);
    }
}
```

---

## 5. Verification Method (Phương pháp kiểm chứng độc lập)

1. **Kiểm tra biên dịch mã nguồn**:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   ./mvnw test-compile
   ```
   - Điều kiện đạt: Lệnh hoàn tất với mã thoát `0` (`BUILD SUCCESS`), 0 compilation errors.
2. **Kiểm tra tính toàn vẹn của Mockito trong Unit Tests**:
   - Khi chạy Unit Tests của Use Case (M2, M3), các ports `VendorPort` và `AuditLogPort` có thể được mock trực tiếp thông qua Mockito:
     `VendorPort vendorPort = mock(VendorPort.class);`
     `AuditLogPort auditLogPort = mock(AuditLogPort.class);`
   - Không cần kích hoạt Spring context hay khởi động Testcontainers/Docker khi kiểm thử Unit Use Cases.
3. **Kiểm tra Clean Architecture Rule Compliance**:
   - Kiểm tra các import statements trong `com.danasea.backend.modules.service.domain.*`:
     Không được phép chứa bất kỳ import nào từ `org.springframework.*`, `jakarta.persistence.*`, hay `com.danasea.backend.modules.service.infrastructure.*`.
   - Các UseCases trong `application.usecases` chỉ import các interfaces từ `service.domain.ports` (bao gồm `VendorPort` và `AuditLogPort`), không import JPA entities hay Spring Data repositories.
4. **Điều kiện vô hiệu hóa (Invalidation Conditions)**:
   - Nếu xuất hiện dependency trực tiếp từ `service.domain` sang JPA repository của `vendor` hoặc `account`.
   - Nếu `AccountInternalApi` hoặc `VendorInternalApi` ném checked exceptions hoặc làm lộ JPA entities (`VendorJpaEntity`, `AuditLogJpaEntity`) sang module `service`.
