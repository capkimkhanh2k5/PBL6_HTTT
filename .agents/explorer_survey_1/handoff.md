# Báo cáo Khảo sát Hiện trạng Codebase cho Module Services (explorer_survey_1)

## 1. Observation (Quan sát thực tế)

### 1.1. Công nghệ nền tảng & Build Tool
- **Ngôn ngữ & Runtime**: Java 21 LTS (`OpenJDK Runtime Environment Temurin-21.0.10+7-LTS` tại `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`).
- **Build Tool**: Maven với Maven Wrapper `./mvnw` (`backend/pom.xml`, `backend/mvnw`).
- **Framework**: Spring Boot (`<artifactId>spring-boot-starter-parent</artifactId>`, `<version>4.1.1</version>` tại `backend/pom.xml:7-8`).
- **Các thư viện chính** (`backend/pom.xml`):
  - Spring Data JPA (`spring-boot-starter-data-jpa`)
  - Spring Security (`spring-boot-starter-security`)
  - Spring WebMVC (`spring-boot-starter-webmvc`)
  - Spring Validation (`spring-boot-starter-validation`)
  - PostgreSQL Driver (`org.postgresql:postgresql:42.7.13`)
  - JJWT (`io.jsonwebtoken:jjwt-api:0.13.0`)
  - Lombok (`org.projectlombok:lombok`)
  - Springdoc OpenAPI (`springdoc-openapi-starter-webmvc-ui:3.1.1`)
  - Redis (`spring-boot-starter-data-redis`), RabbitMQ (`spring-boot-starter-amqp`), Bucket4j.
  - Test dependencies: `spring-boot-starter-test`, `testcontainers-junit-jupiter`, `testcontainers-redis`.

### 1.2. Cơ chế Database Migration & Schema
- Không tìm thấy bất kỳ file migration SQL, Flyway hoặc Liquibase nào trong `backend/src/main/resources` (không có thư mục `db/migration` hay file `.sql`).
- Cấu hình JPA tại `backend/src/main/resources/application.yml:14-17`:
  ```yaml
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
  ```
  Hệ thống quản lý schema tự động thông qua JPA Entity với `hibernate.ddl-auto: update` kết nối PostgreSQL.

### 1.3. Kiến trúc hệ thống (Clean Architecture / Modular Monolith)
Theo `backend/docs/Clean_Architecture_Rules.md` và cấu trúc mã nguồn:
- Phân chia theo module nghiệp vụ tại `com.danasea.backend.modules`:
  - `account`, `ai`, `communication`, `operation`, `order`, `service`, `systemconfig`, `vendor`, `weather`.
- Mỗi module tổ chức theo 4 tầng Clean Architecture:
  - `domain/`: Chứa Domain Models, Enums, Domain Exceptions, Domain Ports/Events (độc lập công nghệ).
  - `application/`: Chứa Use Cases, DTOs (Commands/Results), Internal APIs (ví dụ: `AccountInternalApi`).
  - `infrastructure/`: Chứa JPA Entities, Spring Data Repositories, Adapters, Mappers, External Services.
  - `presentation/`: Chứa REST Controllers, Request/Response DTOs, Exception Handlers.
- Các use cases được đăng ký thành Spring Beans thông qua class `@Configuration` (ví dụ `com.danasea.backend.config.ApplicationBeans` và `AuthorizationBeans`), tách biệt tầng application khỏi annotation framework.
- Tầng bảo mật và dùng chung:
  - `com.danasea.backend.shared.core.domain.models.BaseDomainModel` (`id`, `createdAt`, `updatedAt`).
  - `com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity` (UUID id, created_at, updated_at).
  - `com.danasea.backend.shared.presentation.ErrorResponse` (`record ErrorResponse(String code, String message)`).
  - `com.danasea.backend.security.authentication`: JWT Filter, đăng nhập, đăng ký, OTP.
  - `com.danasea.backend.security.authorization`: RBAC, phân quyền role, permission, `SecurityConfig`.

### 1.4. Khảo sát chi tiết Module `service` (`com.danasea.backend.modules.service`)
Hiện trạng:
- **Domain Models (`modules/service/domain/models/`)**:
  - `Service.java`: Kế thừa `BaseDomainModel`, chứa các trường: `vendorId`, `categoryId`, `name`, `nameEn`, `slug`, `description`, `descriptionEn`, `price`, `durationMinutes`, `capacityPerSlot`, `locationName`, `address`, `latitude`, `longitude`, `status`, `waiverContent`, `weatherSensitive`, `minWindKmh`, `maxWaveM`, `avgRating`, `ratingCount`, `viewCount`.
  - `ServiceStatus.java`: Enum gồm `DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `REJECTED`, `PAUSED`.
  - `ServiceImage.java`: Kế thừa `BaseDomainModel`, chứa: `serviceId`, `url`, `sortOrder`.
  - `Category.java`: Kế thừa `BaseDomainModel`, chứa: `name`, `nameEn`, `slug`, `parentId`, `iconUrl`, `isActive`.
  - Khác: `DocStatus.java`, `RecentlyViewed.java`, `ServiceSafetyDocument.java`, `ServiceSlot.java`, `SlotStatus.java`, `Wishlist.java`.
- **JPA Entities (`modules/service/infrastructure/persistence/entities/`)**:
  - `ServiceJpaEntity.java` (`@Table(name = "services")`): Đầy đủ các cột khớp `Service.java`.
  - `ServiceImageJpaEntity.java` (`@Table(name = "service_images")`): Khớp `ServiceImage.java`.
  - `CategoryJpaEntity.java` (`@Table(name = "categorys")`): Khớp `Category.java`.
- **Repositories (`modules/service/infrastructure/persistence/repositories/`)**:
  - `JpaServiceRepository.java`: Mới chỉ kế thừa `JpaRepository<ServiceJpaEntity, UUID>`, chưa có query methods.
  - `JpaServiceImageRepository.java`: Mới chỉ kế thừa `JpaRepository<ServiceImageJpaEntity, UUID>`, chưa có query methods tìm theo `serviceId`.
  - `JpaCategoryRepository.java`: Mới chỉ kế thừa `JpaRepository<CategoryJpaEntity, UUID>`.
- **Tầng còn thiếu hoàn toàn trong `service`**:
  - Thiếu tầng `application/`: Chưa có use cases (`CreateServiceUseCase`, `SubmitServiceForReviewUseCase`, `UpdateServiceUseCase`, `ApproveServiceUseCase`, `RejectServiceUseCase`, `DeleteServiceUseCase`, v.v.), chưa có Ports hay DTOs.
  - Thiếu tầng `presentation/`: Chưa có REST Controllers (`/api/vendor/services`, `/api/admin/services`), chưa có DTOs và ExceptionHandler.
  - Thiếu mappers (`ServiceMapper`, `ServiceImageMapper`, `CategoryMapper`).
  - Thiếu cấu hình bean (`ServiceBeans`).

### 1.5. Khảo sát Module `vendor` (`com.danasea.backend.modules.vendor`)
- `Vendor.java` (`domain/models/Vendor.java`): Chứa `userId`, `businessName`, `taxCode`, `address`, `verificationStatus`, `verifiedBy`, `verifiedAt`, `ratingAvg`, `ratingCount`, `badgeTier`.
- `VerificationStatus.java`: Enum `PENDING, APPROVED, REJECTED`.
- `VendorJpaEntity.java` (`@Table(name = "vendors")`).
- `JpaVendorRepository.java`: Chưa có phương thức `findByUserId(UUID userId)`.
- Module `vendor` chưa có tầng `application/`, chưa có `VendorInternalApi` để module `service` truy vấn kiểm tra quyền tạo dịch vụ của Vendor theo `userId`.

### 1.6. Khảo sát Module `account` & `AuditLog`
- `AuditLog.java` (`modules/account/domain/models/AuditLog.java`): Chứa `actorUserId`, `action`, `entityType`, `entityId`, `metadata`.
- `AuditLogJpaEntity.java` (`@Table(name = "audit_logs")`).
- `JpaAuditLogRepository.java`.
- `AccountInternalApi.java` hiện chưa có hàm ghi `AuditLog` cho các hành động Admin duyệt/từ chối dịch vụ.

### 1.7. Khảo sát Bảo mật & Phân quyền (Security)
- `SecurityConfig.java`:
  - Đang cấu hình `.requestMatchers("/api/auth/**", ...).permitAll()` và `.anyRequest().authenticated()`.
  - Có `@EnableMethodSecurity`.
  - Access Denied Handler trả về 403 với format JSON: `{"code":"ACCESS_DENIED","message":"Access denied"}`.
  - Hiện tại chưa có rule tường minh cho `/api/vendor/**` và `/api/admin/**` trong `SecurityConfig` (cần thêm matcher hoặc dùng `@PreAuthorize("hasRole('VENDOR')")` / `@PreAuthorize("hasRole('ADMIN')")`).

### 1.8. Kiểm chứng môi trường Build & Test
- Đã chạy `./mvnw test-compile`: Thành công biên dịch 194 source files (`BUILD SUCCESS`).
- Đã chạy kiểm thử với `./mvnw test -Dtest=LoginUseCaseTest`: PASS 5/5 tests (yêu cầu cấu hình `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`).
- Đã chạy kiểm thử MockMvc với `./mvnw test -Dtest=AuthenticationControllerTest`: PASS 7/7 tests.

---

## 2. Logic Chain (Chuỗi lập luận và phân tích)

1. **Về kiến trúc & tổ chức package**:
   - Từ Quan sát 1.3 và tài liệu `Clean_Architecture_Rules.md`, việc triển khai module `service` phải tuân thủ chuẩn Modular Clean Architecture đã có trong codebase:
     - `modules/service/domain`: Chứa model (`Service`, `Category`, `ServiceImage`), enums (`ServiceStatus`), domain exceptions, repository ports.
     - `modules/service/application`: Chứa các Use Cases (`CreateServiceUseCase`, `SubmitServiceForReviewUseCase`, `UpdateServiceUseCase`, `ApproveServiceUseCase`, `RejectServiceUseCase`, `DeleteServiceUseCase`, `GetVendorServicesUseCase`, `GetAdminServicesUseCase`, v.v.), input/output records.
     - `modules/service/infrastructure`: Mappers (`ServiceMapper`, `ServiceImageMapper`), persistence adapters hiện thực repository ports, và Spring bean configuration (`ServiceBeans`).
     - `modules/service/presentation`: `VendorServiceController` (`/api/vendor/services`), `AdminServiceController` (`/api/admin/services`), request DTOs, `ServiceExceptionHandler`.
   
2. **Về quan hệ giữa Service và Vendor (R1, R3)**:
   - Quan sát 1.4 & 1.5 chỉ ra rằng `Service` lưu `vendorId` (UUID của bảng `vendors`).
   - Khi Vendor đăng nhập, thông tin định danh trong Spring Security Principal là `email`. Từ `email`, có thể lấy `User.id`.
   - Do đó, để validate quyền Vendor và kiểm tra sở hữu (ownership), cần liên kết từ `userId` sang `Vendor`.
   - Vì module `vendor` chưa có `VendorInternalApi` và `JpaVendorRepository.findByUserId(UUID userId)` (Quan sát 1.5), cần bổ sung phương thức này và cung cấp `VendorInternalApi` (hoặc Port tương ứng) để module `service` tra cứu Vendor và kiểm tra `vendor.getVerificationStatus() == VerificationStatus.APPROVED`.

3. **Về các Business Rules (R3)**:
   - **Tạo dịch vụ**:
     - Kiểm tra Vendor: `verification_status == APPROVED`. Nếu đang `PENDING` hoặc không phải APPROVED -> ném lỗi nghiệp vụ (trả về 400 hoặc 403 tùy quy định, ví dụ `VENDOR_NOT_APPROVED`).
     - Trạng thái ban đầu: `DRAFT`.
     - Kiểm tra `category_id`: Gọi `categoryRepository.findById(categoryId)`. Nếu không tồn tại -> 404 (`CATEGORY_NOT_FOUND`). Nếu `isActive == false` -> 400 (`CATEGORY_INACTIVE`).
     - Kiểm tra thời tiết: Nếu `weather_sensitive == true`, bắt buộc `min_wind_kmh != null` và `max_wave_m != null`. Nếu thiếu -> 400 (`INVALID_WEATHER_DATA`).
   - **Gửi duyệt (`/submit`)**:
     - Cho phép chuyển từ `DRAFT` hoặc `REJECTED` -> `PENDING_REVIEW`.
     - Phải kiểm tra ảnh: Tra cứu `service_images` theo `serviceId`. Nếu danh sách rỗng -> 400 (`SERVICE_IMAGES_REQUIRED`).
     - Nếu trạng thái khác `DRAFT` và `REJECTED` (ví dụ đang `PUBLISHED` hoặc `PENDING_REVIEW`) -> 400 (`INVALID_SERVICE_STATUS`).
   - **Chỉnh sửa dịch vụ (`PATCH /{id}`)**:
     - Kiểm tra quyền sở hữu: `service.vendorId == currentVendor.id`. Nếu khác -> 403 (`FORBIDDEN` / `UNAUTHORIZED_SERVICE_ACCESS`).
     - Nếu đang `DRAFT`: Cập nhật tự do, giữ nguyên `DRAFT`.
     - Nếu đang `PUBLISHED`: Cho phép cập nhật, nhưng tự động chuyển trạng thái về `PENDING_REVIEW`.
     - Nếu đang `REJECTED`: Cho phép cập nhật thông tin để chuẩn bị gửi duyệt lại.
   - **Tạm ngưng / Mở lại**:
     - Pause (`PATCH /{id}/pause`): Chỉ khi đang `PUBLISHED` -> chuyển sang `PAUSED`.
     - Resume (`PATCH /{id}/resume`): Chỉ khi đang `PAUSED` -> chuyển sang `PUBLISHED`.
   - **Xóa dịch vụ (`DELETE /{id}`)**:
     - Kiểm tra quyền sở hữu: chỉ xóa dịch vụ của chính mình (403 nếu sai vendor).
     - Chỉ cho phép xóa khi status = `DRAFT`.
     - Chặn và báo lỗi 400 rõ ràng nếu cố xóa khi đang `PUBLISHED` hoặc `PAUSED` (hoặc `PENDING_REVIEW`).

4. **Về Admin duyệt dịch vụ & Audit Log (R2)**:
   - `GET /api/admin/services?status=PENDING_REVIEW`: Lọc danh sách dịch vụ theo trạng thái (mặc định hoặc query param `PENDING_REVIEW`).
   - `PATCH /api/admin/services/{id}/approve`:
     - Kiểm tra dịch vụ có status == `PENDING_REVIEW` -> chuyển sang `PUBLISHED`.
     - Ghi nhận Audit Log: `actorUserId = adminUserId`, `action = "SERVICE_APPROVED"`, `entityType = "SERVICE"`, `entityId = serviceId`.
   - `PATCH /api/admin/services/{id}/reject`:
     - Request body bắt buộc có `reason`.
     - Chuyển status -> `REJECTED`.
     - Ghi nhận Audit Log: `actorUserId = adminUserId`, `action = "SERVICE_REJECTED"`, `entityType = "SERVICE"`, `entityId = serviceId`, `metadata = reason` (hoặc JSON chứa lý do).

5. **Về Security & Phân quyền (R4)**:
   - `/api/vendor/services/**`:
     - Yêu cầu Role `VENDOR`. Chặn `CUSTOMER`, `ADMIN` (trả về 403).
   - `/api/admin/services/**`:
     - Yêu cầu Role `ADMIN`. Chặn `VENDOR`, `CUSTOMER` (trả về 403).
   - Giải pháp tối ưu: Kết hợp cả `@PreAuthorize("hasRole('VENDOR')")` / `@PreAuthorize("hasRole('ADMIN')")` trên controller class và cấu hình bổ sung trong `SecurityConfig.java`:
     ```java
     .requestMatchers("/api/vendor/**").hasRole("VENDOR")
     .requestMatchers("/api/admin/**").hasRole("ADMIN")
     ```

---

## 3. Caveats (Các điểm cần lưu ý & Giả định)

1. **Môi trường chạy Java**: Khi chạy lệnh `./mvnw` từ terminal, cần export `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` vì JDK không nằm ở PATH mặc định của shell.
2. **Mockito Agent**: Chạy kiểm thử Mockito trong môi trường macOS sandbox cần cấu hình phù hợp hoặc chạy với quyền cho phép attachment (như đã xác nhận thành công với các test hiện tại).
3. **Audit Log Persistence**: Module `account` hiện đã có `AuditLogJpaEntity` và `JpaAuditLogRepository` nhưng chưa có hàm trong `AccountInternalApi`. Có thể mở rộng `AccountInternalApi` với `void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata)` hoặc tạo `AuditLogPort` trong tầng application của service.
4. **Bảng Category**: Tên bảng trong JPA Entity hiện tại là `@Table(name = "categorys")`. Cần giữ nguyên ánh xạ này để tránh lệch schema.

---

## 4. Conclusion (Kết luận & Khuyến nghị hành động)

### 4.1. Đánh giá hiện trạng
- Nền tảng dự án rất sạch, tuân thủ nghiêm ngặt mô hình Modular Clean Architecture.
- Các Entity, Model, Enum cốt lõi (`Service`, `ServiceStatus`, `ServiceImage`, `Category`, `Vendor`, `VerificationStatus`, `AuditLog`, `Role`) đã sẵn sàng.
- Tầng nghiệp vụ và giao tiếp API của Module `service` chưa có gì, cần được xây dựng mới hoàn toàn.

### 4.2. Danh sách các thành phần cần triển khai cho Module `service`
1. **Domain Layer**:
   - `modules/service/domain/exception/`: Các domain exceptions (`ServiceNotFoundException`, `CategoryNotFoundException`, `CategoryInactiveException`, `VendorNotApprovedException`, `InvalidServiceStatusException`, `MissingServiceImagesException`, `InvalidWeatherDataException`, `UnauthorizedServiceAccessException`).
   - `modules/service/domain/port/`: `ServiceRepositoryPort`, `ServiceImageRepositoryPort`, `CategoryRepositoryPort`, `AuditLogPort`, `VendorPort`.
2. **Application Layer**:
   - `modules/service/application/dto/`: Commands và Results cho Create, Update, Reject, v.v.
   - `modules/service/application/usecase/`:
     - `CreateServiceUseCase`
     - `SubmitServiceForReviewUseCase`
     - `UpdateServiceUseCase`
     - `ApproveServiceUseCase`
     - `RejectServiceUseCase`
     - `DeleteServiceUseCase`
     - `GetVendorServicesUseCase`, `GetVendorServiceDetailUseCase`
     - `PauseServiceUseCase`, `ResumeServiceUseCase`
     - `GetAdminServicesUseCase`
3. **Infrastructure Layer**:
   - Mappers: `ServiceMapper`, `ServiceImageMapper`, `CategoryMapper`.
   - Repository query methods bổ sung trong `JpaServiceRepository`, `JpaServiceImageRepository`, `JpaVendorRepository`.
   - Adapters hiện thực các Ports.
   - `ServiceBeans.java`: Đăng ký use case beans vào Spring Context.
4. **Presentation Layer**:
   - `VendorServiceController`: Các endpoints `/api/vendor/services/**` (`@PreAuthorize("hasRole('VENDOR')")`).
   - `AdminServiceController`: Các endpoints `/api/admin/services/**` (`@PreAuthorize("hasRole('ADMIN')")`).
   - Request DTOs với validation annotations (`@NotBlank`, `@NotNull`, `@Min`, v.v.).
   - `ServiceExceptionHandler`: Bắt các domain exceptions và trả về mã lỗi HTTP chuẩn (400, 403, 404).
5. **Security Configuration**:
   - Cập nhật `SecurityConfig.java` để phân quyền endpoint `/api/vendor/**` và `/api/admin/**`.
6. **Test Suite**:
   - 6 Use Case Unit Tests: `CreateServiceUseCaseTest`, `SubmitServiceForReviewUseCaseTest`, `UpdateServiceUseCaseTest`, `ApproveServiceUseCaseTest`, `RejectServiceUseCaseTest`, `DeleteServiceUseCaseTest`.
   - MockMvc Tests: `ServiceControllerTest` kiểm tra toàn bộ luồng RBAC phân quyền (Vendor, Admin, Customer, Unauthenticated).

---

## 5. Verification Method (Phương pháp kiểm chứng)

Sau khi hoàn thành triển khai, có thể kiểm chứng độc lập bằng các bước sau:

1. **Kiểm tra biên dịch dự án**:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   ./mvnw clean test-compile
   ```
   Kết quả mong đợi: `BUILD SUCCESS`, không có lỗi biên dịch.

2. **Chạy toàn bộ Use Case Unit Tests**:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   ./mvnw test -Dtest=CreateServiceUseCaseTest,SubmitServiceForReviewUseCaseTest,UpdateServiceUseCaseTest,ApproveServiceUseCaseTest,RejectServiceUseCaseTest,DeleteServiceUseCaseTest
   ```
   Kết quả mong đợi: Toàn bộ 6 test suites đều PASS.

3. **Chạy MockMvc Controller Tests kiểm tra phân quyền**:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   ./mvnw test -Dtest=ServiceControllerTest
   ```
   Kết quả mong đợi: Tất cả các case phân quyền (Vendor truy cập thành công endpoint vendor, Customer/Admin bị chặn 403; Admin truy cập thành công endpoint admin, Vendor/Customer bị chặn 403) đều PASS.
