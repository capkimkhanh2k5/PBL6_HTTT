# BÁO CÁO KHẢO SÁT TOÀN DIỆN CODEBASE: CLOUDINARY & SERVICE IMAGES (R1)

**Dự án:** DANASEA (PBL6_HTTT)  
**Người thực hiện:** Explorer Survey 1  
**Ngày hoàn thành:** 2026-09-10  
**Thư mục làm việc:** `.agents/teamwork_preview_explorer_survey_1`  

---

## 1. TỔNG QUAN CÔNG NGHỆ VÀ KIẾN TRÚC CODEBASE

### 1.1. Ngôn ngữ & Framework
- **Ngôn ngữ:** Java 21 (LTS)
- **Framework chính:** Spring Boot 4.1.1
  - `spring-boot-starter-webmvc`: Cung cấp RESTful web framework, Servlet container, và multipart resolver.
  - `spring-boot-starter-data-jpa`: Tương tác với cơ sở dữ liệu quan hệ qua Hibernate / Spring Data JPA.
  - `spring-boot-starter-security`: Bảo mật, xác thực qua JWT (`jjwt:0.13.0`), phân quyền theo role (`CUSTOMER`, `VENDOR`, `ADMIN`).
  - `spring-boot-starter-validation`: Jakarta Bean Validation (`@Valid`, `@NotNull`, etc.).
  - `spring-boot-starter-data-redis`: Cache và Bucket4j rate limiting.
  - `spring-boot-starter-amqp`: RabbitMQ event-driven messaging.
- **Build tool:** Maven (`backend/pom.xml`, Maven Wrapper `./mvnw`).
- **Database:** PostgreSQL 17 (chạy qua Docker Compose, cấu hình Hibernate `ddl-auto: update`).
- **API Documentation:** Springdoc OpenAPI 3 (`springdoc-openapi-starter-webmvc-ui:3.1.1`).
- **Testing:** JUnit 5 (`org.junit.jupiter`), Mockito (`org.mockito`), Spring Boot Test.

### 1.2. Cấu trúc thư mục dự án
Hệ thống được tổ chức theo mô hình **Modular Clean Architecture** (quy định tại `backend/docs/Clean_Architecture_Rules.md`):
```text
backend/src/main/java/com/danasea/backend/
├── config/                     # Cấu hình chung (SecurityConfig, ApplicationBeans, OpenApiConfig, RabbitMQConfig)
├── security/                   # Module bảo mật trung tâm
│   ├── authentication/         # Xác thực JWT, OTP, Login/Register (port, usecase, domain, infra, presentation)
│   └── authorization/          # Phân quyền, ownership check (policy, port, adapter)
├── shared/                     # Mã nguồn dùng chung
│   ├── core/domain/models/     # BaseDomainModel (id, createdAt, updatedAt)
│   ├── core/infrastructure/    # BaseJpaEntity (UUID id, createdAt, updatedAt)
│   └── presentation/           # ErrorResponse record (code, message)
└── modules/                    # Các module nghiệp vụ độc lập
    ├── account/                # Quản lý người dùng, refreshToken, UserInternalApi
    ├── service/                # Quản lý dịch vụ, ảnh dịch vụ, tài liệu an toàn, danh mục
    ├── vendor/                 # Quản lý nhà cung cấp, hồ sơ vendor
    ├── order/                  # Đơn hàng, thanh toán, hoàn tiền
    ├── operation/              # Đánh giá, đối soát, payout
    ├── communication/          # Thông báo, tin nhắn, khiếu nại
    ├── weather/                # Dữ liệu thời tiết, đánh giá an toàn thời tiết
    ├── ai/                     # Trợ lý AI du lịch
    └── systemconfig/           # Cấu hình hệ thống
```

---

## 2. HIỆN TRẠNG CLOUDINARY & GIẢI PHÁP TÍCH HỢP

### 2.1. Phát hiện quan trọng (Finding)
- **Codebase hiện tại HOÀN TOÀN CHƯA CÓ Cloudinary**:
  - Không có dependency Cloudinary trong `backend/pom.xml`.
  - Không có thông tin cấu hình Cloudinary trong `backend/src/main/resources/application.yml` hay `.env.example`.
  - Không có bất kỳ helper, utility, hay adapter nào cho Cloudinary.
- **Tài liệu đặc tả liên quan:**
  - `docs/DANASEA.docx` xác nhận: *"Lưu trữ tệp/hình ảnh: Cloudinary: Lưu trữ hình ảnh dịch vụ, hồ sơ nhà cung cấp và ảnh đánh giá tách khỏi máy chủ ứng dụng và cơ sở dữ liệu."*
  - `backend/docs/DANASEA_Database_Design.docx` xác định: `service_images.url TEXT NOT NULL — lưu trên Cloudinary`.

### 2.2. Phương án tích hợp Cloudinary theo Clean Architecture
Để tuân thủ tuyệt đối quy tắc phụ thuộc (Dependency Rule) của Clean Architecture:
1. **Bổ sung dependency vào `backend/pom.xml`**:
   ```xml
   <dependency>
       <groupId>com.cloudinary</groupId>
       <artifactId>cloudinary-http44</artifactId>
       <version>1.39.0</version>
   </dependency>
   ```
2. **Cấu hình môi trường (`application.yml` & `.env.example`)**:
   ```yaml
   cloudinary:
     cloud-name: ${CLOUDINARY_CLOUD_NAME:}
     api-key: ${CLOUDINARY_API_KEY:}
     api-secret: ${CLOUDINARY_API_SECRET:}
   ```
3. **Thiết kế Port (Application Layer)**:
   - Tạo interface `ImageStoragePort` (hoặc `CloudinaryPort`) thuộc `com.danasea.backend.modules.service.application.port`:
     ```java
     public interface ImageStoragePort {
         String uploadImage(byte[] fileBytes, String originalFilename, String folder);
         void deleteImage(String publicIdOrUrl);
     }
     ```
   - **Lợi ích kiến trúc:** Tách biệt hoàn toàn Use Case khỏi thư viện bên thứ ba. Trong Unit Tests (`UploadServiceImageUseCaseTest`), chỉ cần `mock(ImageStoragePort.class)` mà không cần gọi mạng hoặc cấu hình API key thực tế.
4. **Thiết kế Adapter (Infrastructure Layer)**:
   - Tạo `CloudinaryStorageAdapter` implements `ImageStoragePort` trong `com.danasea.backend.modules.service.infrastructure.storage`:
     - Sử dụng `com.cloudinary.Cloudinary` bean.
     - Gọi `cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap("folder", folder, "resource_type", "image"))`.
     - Lấy `secure_url` trả về.
5. **Cấu hình Bean (`CloudinaryConfig`)**:
   - Khởi tạo bean `Cloudinary` bằng cách đọc credentials từ `Environment` hoặc `@Value`.

---

## 3. KHẢO SÁT DATABASE, ORM & CẤU TRÚC MODEL

### 3.1. Bảng & Entity `services`
- **File domain model:** `com.danasea.backend.modules.service.domain.models.Service`
- **File JPA entity:** `com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity` (`@Table(name = "services")`)
- **Các thuộc tính chính:**
  - `id`: UUID (PK, sinh tự động UUID v4 qua `BaseJpaEntity`)
  - `vendorId`: UUID (liên kết logic tới `vendors.id`)
  - `categoryId`: UUID (liên kết logic tới `categorys.id`)
  - `name`, `nameEn`, `slug`: Tên và đường dẫn slug
  - `price`: BigDecimal
  - `status`: Enum `ServiceStatus` (`DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `REJECTED`, `PAUSED`)
  - `weatherSensitive`: Boolean
  - `createdAt`, `updatedAt`: OffsetDateTime từ `BaseJpaEntity`

### 3.2. Bảng & Entity `service_images`
- **File domain model:** `com.danasea.backend.modules.service.domain.models.ServiceImage`
  ```java
  public class ServiceImage extends BaseDomainModel {
      private UUID serviceId;
      private String url;
      private Short sortOrder;
  }
  ```
- **File JPA entity:** `com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity` (`@Table(name = "service_images")`)
  ```java
  @Entity
  @Getter
  @Setter
  @Table(name = "service_images")
  public class ServiceImageJpaEntity extends BaseJpaEntity {
      private UUID serviceId;
      private String url;
      private Short sortOrder;
  }
  ```
- **Repository hiện tại:** `com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceImageRepository`
  - Hiện là interface rỗng kéo dài `JpaRepository<ServiceImageJpaEntity, UUID>`.
  - **Cần bổ sung các query method:**
    ```java
    List<ServiceImageJpaEntity> findByServiceIdOrderBySortOrderAsc(UUID serviceId);
    long countByServiceId(UUID serviceId);
    Optional<ServiceImageJpaEntity> findByIdAndServiceId(UUID id, UUID serviceId);
    @Query("SELECT COALESCE(MAX(si.sortOrder), -1) FROM ServiceImageJpaEntity si WHERE si.serviceId = :serviceId")
    Short findMaxSortOrderByServiceId(@Param("serviceId") UUID serviceId);
    void deleteByIdAndServiceId(UUID id, UUID serviceId);
    ```

### 3.3. Bảng `categorys` và cờ an toàn theo Category (R3)
- **File JPA entity:** `CategoryJpaEntity` (`@Table(name = "categorys")`)
- **File domain model:** `Category`
- **Hiện trạng:** Đang có `name`, `nameEn`, `slug`, `parentId`, `iconUrl`, `isActive`.
- **Yêu cầu bổ sung cho R3:** Cần thêm trường `requiresSafetyCert` (kiểu `Boolean`, mặc định `false`) vào cả `Category` domain model và `CategoryJpaEntity`.

---

## 4. KHẢO SÁT FILE UPLOAD MULTIPART/FORM-DATA

### 4.1. Cơ chế trong Spring Boot
- Dự án sử dụng Spring MVC tiêu chuẩn.
- Cần khai báo cấu hình upload trong `backend/src/main/resources/application.yml`:
  ```yaml
  spring:
    servlet:
      multipart:
        enabled: true
        max-file-size: 10MB
        max-request-size: 10MB
  ```
- Tại Controller, nhận request upload:
  ```java
  @PostMapping(value = "/api/vendor/services/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ServiceImageResponse> uploadImage(
          @PathVariable("id") UUID serviceId,
          @RequestParam("file") MultipartFile file,
          Principal principal
  )
  ```

### 4.2. File Validation & Business Rules cho R1
1. **Kiểm tra rỗng:** `file == null || file.isEmpty()` -> ném `InvalidFileException("File must not be empty")` (HTTP 400).
2. **Kiểm tra Content-Type (MIME type):**
   - Chỉ chấp nhận: `image/jpeg`, `image/png`, `image/webp`.
   - Nếu contentType khác hoặc null -> ném `InvalidFileFormatException("Only JPEG, PNG, and WEBP images are supported")` (HTTP 400).
3. **Kiểm tra giới hạn số ảnh tối đa (Max Images Limit):**
   - Đặt hằng số `MAX_IMAGES_PER_SERVICE = 10` (hoặc cấu hình qua properties).
   - Trước khi upload lên Cloudinary, đếm số ảnh hiện có: `imageRepo.countByServiceId(serviceId)`.
   - Nếu `count >= MAX_IMAGES_PER_SERVICE` -> ném `MaxServiceImagesExceededException("Maximum 10 images allowed per service")` (HTTP 400).
4. **Tính toán `sort_order` tự động tăng:**
   - Tìm giá trị lớn nhất: `Short maxSort = imageRepo.findMaxSortOrderByServiceId(serviceId)`.
   - Nếu chưa có ảnh nào (`maxSort == -1` hoặc `null`), set `newSortOrder = 0`.
   - Nếu đã có ảnh, `newSortOrder = (short) (maxSort + 1)`.

---

## 5. CƠ CHẾ BẢO MẬT & PHÂN QUYỀN (SECURITY & VENDOR OWNERSHIP)

1. **Xác thực JWT:**
   - `JwtAuthenticationFilter` phân giải Bearer token từ header `Authorization`.
   - Đặt `subject.email()` vào SecurityContext principal và gán authorities `ROLE_VENDOR`, `ROLE_ADMIN`, ...
2. **Kiểm tra quyền sở hữu (Ownership Check):**
   - Khi Vendor thực hiện các hành động trên service (`/api/vendor/services/{id}/**`):
     - Bước 1: Lấy thông tin user hiện tại qua email (`AccountInternalApi.findUserByEmail(principal.getName())`).
     - Bước 2: Tìm hồ sơ vendor tương ứng qua `userId` (`JpaVendorRepository.findByUserId(user.getId())`).
     - Bước 3: Tìm service theo `serviceId`. Nếu không tìm thấy -> ném `ServiceNotFoundException` (HTTP 404).
     - Bước 4: So sánh `service.getVendorId()` với `vendor.getId()`. Nếu không khớp (và user không phải ADMIN) -> ném `AccessDeniedException` hoặc `UnauthorizedServiceAccessException` (HTTP 403).

---

## 6. THIẾT KẾ CHI TIẾT CÁC USE CASE R1

### 6.1. Use Case 1: `UploadServiceImageUseCase`
- **Mục tiêu:** Upload ảnh mới cho service lên Cloudinary, tự động gán `sort_order`.
- **Luồng xử lý:**
  1. Xác thực quyền sở hữu: kiểm tra vendor sở hữu service (hoặc 403 / 404).
  2. Validate file (rỗng, sai MIME type) -> 400.
  3. Kiểm tra số lượng ảnh hiện có < `MAX_IMAGES_PER_SERVICE` -> 400 nếu vượt quá.
  4. Tính `nextSortOrder = currentMaxSortOrder + 1`.
  5. Gọi `ImageStoragePort.uploadImage(fileBytes, filename, folder)`.
  6. Lưu `ServiceImageJpaEntity` vào DB.
  7. Trả về `ServiceImageResponse` (id, serviceId, url, sortOrder, createdAt).

### 6.2. Use Case 2: `DeleteServiceImageUseCase`
- **Mục tiêu:** Xóa ảnh của service.
- **Luồng xử lý:**
  1. Xác thực quyền sở hữu service của vendor -> 403 / 404.
  2. Tìm ảnh theo `imageId` và `serviceId`. Nếu không tồn tại hoặc ảnh thuộc service khác -> ném `ServiceImageNotFoundException` (HTTP 404).
  3. Xóa record ảnh khỏi cơ sở dữ liệu (`JpaServiceImageRepository.delete(...)`).
  4. (Tùy chọn) Gọi `ImageStoragePort.deleteImage(url)` để dọn dẹp trên Cloudinary.

### 6.3. Use Case 3: `ReorderServiceImagesUseCase`
- **Mục tiêu:** Thay đổi thứ tự (`sort_order`) hàng loạt các ảnh của service.
- **Request DTO:**
  ```java
  public record ReorderImagesRequest(
      @NotEmpty List<ImageOrderItem> items
  ) {
      public record ImageOrderItem(
          @NotNull UUID imageId,
          @NotNull Short sortOrder
      ) {}
  }
  ```
- **Luồng xử lý:**
  1. Xác thực quyền sở hữu service của vendor -> 403 / 404.
  2. Lấy toàn bộ danh sách ảnh hiện có của service từ DB.
  3. **Kiểm tra tính toàn vẹn (Integrity Check):**
     - Đảm bảo tất cả các `imageId` trong request thuộc về service này.
     - Nếu phát hiện bất kỳ `imageId` nào không tồn tại hoặc thuộc về service khác -> lập tức ném `InvalidImageReorderException` hoặc `AccessDeniedException` (HTTP 403 hoặc 404).
  4. Cập nhật `sort_order` cho từng entity tương ứng.
  5. Lưu thay đổi vào DB trong transaction (`@Transactional`).
  6. Trả về danh sách ảnh đã được sắp xếp mới.

---

## 7. DANH SÁCH FILE CẦN TẠO MỚI HOẶC SỬA ĐỔI CHO R1

### 7.1. File cấu hình & Build (Sửa đổi / Bổ sung)
1. `backend/pom.xml`: Thêm dependency `com.cloudinary:cloudinary-http44:1.39.0`.
2. `backend/src/main/resources/application.yml`: Bổ sung cấu hình `spring.servlet.multipart` và `cloudinary` properties.
3. `.env.example`: Thêm mẫu biến môi trường `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.

### 7.2. Module `modules/service` (Tạo mới & Sửa đổi)
1. **Domain Layer:**
   - `domain/exceptions/ServiceNotFoundException.java` (Tạo mới)
   - `domain/exceptions/ServiceImageNotFoundException.java` (Tạo mới)
   - `domain/exceptions/MaxServiceImagesExceededException.java` (Tạo mới)
   - `domain/exceptions/InvalidFileFormatException.java` (Tạo mới)
   - `domain/exceptions/UnauthorizedServiceAccessException.java` (Tạo mới)
   - `domain/exceptions/InvalidImageReorderException.java` (Tạo mới)
2. **Application Layer:**
   - `application/port/ImageStoragePort.java` (Tạo mới - interface trừu tượng upload/delete)
   - `application/usecase/UploadServiceImageUseCase.java` (Tạo mới)
   - `application/usecase/DeleteServiceImageUseCase.java` (Tạo mới)
   - `application/usecase/ReorderServiceImagesUseCase.java` (Tạo mới)
   - `application/dto/UploadImageCommand.java` (Tạo mới - byte[], filename, contentType)
3. **Infrastructure Layer:**
   - `infrastructure/config/CloudinaryConfig.java` (Tạo mới - Bean Cloudinary)
   - `infrastructure/storage/CloudinaryStorageAdapter.java` (Tạo mới - Implements ImageStoragePort)
   - `infrastructure/persistence/repositories/JpaServiceImageRepository.java` (Sửa đổi - bổ sung query methods)
   - `infrastructure/persistence/repositories/JpaVendorRepository.java` (Sửa đổi - bổ sung `findByUserId`)
   - `infrastructure/mapper/ServiceImageMapper.java` (Tạo mới)
   - `infrastructure/config/ServiceModuleConfig.java` (Tạo mới - đăng ký Use Cases thành Spring Bean theo chuẩn `Clean_Architecture_Rules.md`)
4. **Presentation Layer:**
   - `presentation/controller/VendorServiceImageController.java` (Tạo mới - API endpoints: POST, DELETE, PATCH reorder)
   - `presentation/dto/ServiceImageResponse.java` (Tạo mới)
   - `presentation/dto/ReorderImagesRequest.java` (Tạo mới)
   - `presentation/advisor/ServiceExceptionHandler.java` (Tạo mới - bắt các exception và trả về `ErrorResponse`)

### 7.3. Test Layer (`backend/src/test/java`)
1. `modules/service/application/usecase/UploadServiceImageUseCaseTest.java` (Tạo mới):
   - Test case 1: Upload thành công -> `sort_order` tự động tăng từ max cũ.
   - Test case 2: Vượt quá số ảnh tối đa (>= 10) -> ném `MaxServiceImagesExceededException`.
   - Test case 3: Định dạng file không hợp lệ -> ném `InvalidFileFormatException`.
   - Test case 4: Service không thuộc quyền sở hữu của vendor -> ném `UnauthorizedServiceAccessException`.
2. `modules/service/application/usecase/ReorderServiceImagesUseCaseTest.java` (Tạo mới):
   - Test case 1: Reorder danh sách hợp lệ thành công -> sortOrder được cập nhật chính xác.
   - Test case 2: Truyền `imageId` của service khác hoặc không tồn tại -> ném exception chặn đứng hành vi giả mạo (trả về 403 hoặc 404).
   - Test case 3: Vendor không sở hữu service -> từ chối thao tác (403).

---

## 8. KẾT LUẬN & KIẾN NGHỊ CHO ORCHESTRATOR
1. Toàn bộ kiến trúc và thiết kế đề xuất ở trên tuân thủ nghiêm ngặt 12 nguyên tắc trong `Clean_Architecture_Rules.md`.
2. Việc sử dụng `ImageStoragePort` giúp cô lập Use Cases hoàn toàn khỏi SDK Cloudinary, đảm bảo khả năng unit test độc lập, không phụ thuộc internet hay mock server phức tạp.
3. Kế hoạch triển khai cho R1 đã được định hình rõ ràng từng file, class, method và test case, sẵn sàng chuyển sang bước implement.
