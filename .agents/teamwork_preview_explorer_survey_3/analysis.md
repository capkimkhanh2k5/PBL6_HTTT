# Báo cáo Khảo sát: Service Lifecycle, Category, Publish Flow & Test Infrastructure

## 1. Tổng quan Khảo sát (Executive Summary)
Khảo sát toàn diện codebase dự án PBL6_HTTT (Danasea Backend) nhằm chuẩn bị triển khai module Service Images & Safety Documents (R1, R2, R3) cùng bộ Test Suite theo Acceptance Criteria.

Dự án là ứng dụng **Spring Boot 4.1.1 (Java 21)** tổ chức theo mô hình **Modular Clean Architecture**:
- Tầng Domain: Pure Java domain models kế thừa `BaseDomainModel` (`id`, `createdAt`, `updatedAt`).
- Tầng Application: Use cases điều phối nghiệp vụ, định nghĩa Port/API contract giữa các module.
- Tầng Infrastructure: JPA entities kế thừa `BaseJpaEntity`, Spring Data JPA repositories, mappers, adapter.
- Tầng Presentation: REST controllers, Exception handlers trả về chuẩn `ErrorResponse(code, message)`.

---

## 2. Khảo sát Category Entity & Cờ `requires_safety_cert` (Mục tiêu 1)

### 2.1. Cấu trúc hiện tại của `Category`
1. **Domain Model** (`backend/src/main/java/com/danasea/backend/modules/service/domain/models/Category.java`):
   - Kế thừa `BaseDomainModel`: `id` (UUID), `createdAt` (OffsetDateTime), `updatedAt` (OffsetDateTime).
   - Các trường riêng:
     - `name`: String (tên danh mục)
     - `nameEn`: String (tên tiếng Anh)
     - `slug`: String (đường dẫn thân thiện)
     - `parentId`: UUID (danh mục cha nếu có phân cấp)
     - `iconUrl`: String (đường dẫn icon)
     - `isActive`: Boolean (trạng thái kích hoạt)

2. **JPA Entity** (`backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/CategoryJpaEntity.java`):
   - Kế thừa `BaseJpaEntity`.
   - Bảng cơ sở dữ liệu: `@Table(name = "categorys")` *(chú ý tên bảng là `categorys`)*.
   - Các trường ánh xạ tương ứng các thuộc tính domain.

3. **Repository** (`backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaCategoryRepository.java`):
   - `JpaRepository<CategoryJpaEntity, UUID>`.

### 2.2. Phương án thêm cờ `requires_safety_cert`
- **Mục đích**: Thay vì hardcode danh sách danh mục rủi ro cao trong mã nguồn, hệ thống gắn cờ động `requires_safety_cert` trên từng category để xác định các dịch vụ thuộc category đó bắt buộc phải có chứng chỉ an toàn được phê duyệt trước khi publish.
- **Thực hiện trong mã nguồn**:
  1. Trong `Category.java`:
     ```java
     private Boolean requiresSafetyCert;
     ```
  2. Trong `CategoryJpaEntity.java`:
     ```java
     @Column(name = "requires_safety_cert", nullable = false)
     private Boolean requiresSafetyCert = false;
     ```
- **Cơ chế Database Migration & DDL**:
  - Trong `backend/src/main/resources/application.yml`:
    ```yaml
    spring:
      jpa:
        hibernate:
          ddl-auto: update
    ```
  - Dự án hiện không sử dụng Liquibase hay Flyway. Hibernate tự động phát hiện thuộc tính mới và sinh câu lệnh:
    ```sql
    ALTER TABLE categorys ADD COLUMN requires_safety_cert boolean DEFAULT false NOT NULL;
    ```
- **Dữ liệu mẫu / Seeders**:
  - Dự án hiện chưa có migration SQL script hoặc seeder class sẵn.
  - Khi triển khai thực tế, các danh mục rủi ro cao như: "Lặn biển (Scuba Diving)", "Mô tô nước (Jet Ski)", "Dù lượn (Parasailing)", "Lướt sóng (Surfing)" sẽ được thiết lập `requires_safety_cert = true`. Các danh mục như "Thuê ô / ghế bãi biển", "Chụp ảnh bãi biển" có `requires_safety_cert = false`.

---

## 3. Khảo sát Service Lifecycle & Publish Flow Logic (Mục tiêu 2)

### 3.1. Các trạng thái vòng đời của Service
Enum `ServiceStatus` (`backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceStatus.java`):
- `DRAFT`: Dịch vụ đang soạn thảo bởi Vendor. Chưa hiển thị trên sàn.
- `PENDING_REVIEW`: Vendor nộp dịch vụ để Admin xét duyệt nội dung / thông tin.
- `PUBLISHED`: Dịch vụ đã công khai, khách hàng có thể tìm kiếm và đặt chỗ.
- `REJECTED`: Dịch vụ bị Admin từ chối phê duyệt.
- `PAUSED`: Dịch vụ tạm dừng hoạt động (bởi Vendor hoặc Admin khi có sự cố thời tiết/vi phạm).

### 3.2. Điều kiện xác định Dịch vụ Rủi ro cao / Nhạy cảm thời tiết
Một dịch vụ được xác định là **bắt buộc phải có chứng chỉ an toàn (Safety Certificate Required)** khi thỏa mãn một trong hai điều kiện:
1. `service.getWeatherSensitive() == true`: Dịch vụ nhạy cảm với thời tiết (cano, lặn ngắm san hô, tour đảo,...).
2. `category.getRequiresSafetyCert() == true`: Thuộc danh mục thể thao biển mạo hiểm / hoạt động có yêu cầu chứng chỉ theo quy định.

Biểu thức kiểm tra:
```java
public boolean isSafetyCertificateRequired(Service service, Category category) {
    return Boolean.TRUE.equals(service.getWeatherSensitive())
        || (category != null && Boolean.TRUE.equals(category.getRequiresSafetyCert()));
}
```

### 3.3. Publish Guard: Quy tắc chặn khi chưa có Safety Document `APPROVED`
- **Quy tắc cốt lõi (Business Rule R3)**:
  - Dịch vụ thuộc diện rủi ro cao (`weatherSensitive == true` hoặc `category.requiresSafetyCert == true`) **bắt buộc phải có ít nhất 1 chứng chỉ an toàn ở trạng thái `APPROVED`** (`DocStatus.APPROVED`).
  - Nếu chưa có safety document nào được `APPROVED` (chưa upload, hoặc chỉ có tài liệu `PENDING`/`REJECTED`), hệ thống **TUYỆT ĐỐI KHÔNG ĐƯỢC PHÉP** chuyển trạng thái service sang `PUBLISHED` (ngay cả khi Admin duyệt service hoặc Vendor bấm publish).
  - Khi cố tình publish vi phạm điều kiện này, use case ném ngoại lệ nghiệp vụ: `SafetyDocumentRequiredException` (mã HTTP 400 Bad Request hoặc 409 Conflict với code `"SAFETY_CERTIFICATE_REQUIRED"`).

### 3.4. Điểm chặn (Enforcement Points) trong Kiến trúc
1. **Trong `PublishServiceUseCase` / `ApproveServiceUseCase`**:
   - Truy vấn thông tin `Service` và `Category`.
   - Kiểm tra `isSafetyCertificateRequired(service, category)`.
   - Nếu `true`, gọi `jpaServiceSafetyDocumentRepository.existsByServiceIdAndStatus(serviceId, DocStatus.APPROVED)`.
   - Nếu trả về `false` -> Ném ngoại lệ, chặn cập nhật trạng thái `PUBLISHED`.
2. **Trong `ApproveSafetyDocumentUseCase`**:
   - Khi Admin duyệt thành công một chứng chỉ an toàn, tài liệu chuyển sang `DocStatus.APPROVED`.
   - Hành động này thỏa mãn điều kiện an toàn, cho phép service đủ điều kiện để chuyển sang `PUBLISHED`.
3. **Trong `RejectSafetyDocumentUseCase` / Xóa tài liệu**:
   - Nếu một tài liệu bị từ chối hoặc thu hồi, kiểm tra xem service còn tài liệu `APPROVED` nào khác không. Nếu không còn và service đang `PUBLISHED`, hệ thống có thể chuyển service về `PAUSED` hoặc `PENDING_REVIEW` để ngăn rủi ro vận hành.

---

## 4. Khảo sát Test Infrastructure & Mocking Patterns (Mục tiêu 3)

### 4.1. Framework & Test Runner
- **Ngôn ngữ & Runtime**: Java 21 LTS (Temurin-21 tại `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`).
- **Test Framework**: JUnit 5 Jupiter (`org.junit.jupiter.api.*`), Spring Boot Starter Test (v4.1.1).
- **Mocking Library**: Mockito (`org.mockito.Mockito.*`, `mockito-junit-jupiter`).
- **Assertion**: JUnit 5 Assertions (`assertEquals`, `assertThrows`, `assertNotNull`, v.v.).
- **Build Tool**: Maven Wrapper (`./mvnw`). Plugin thực thi: `maven-surefire-plugin` (v3.5.6).

### 4.2. Lệnh chạy test
- Chạy toàn bộ test trong dự án:
  ```bash
  JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test
  ```
- Chạy một class test cụ thể:
  ```bash
  JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=LoginUseCaseTest
  ```
- Chạy một method test cụ thể:
  ```bash
  JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=LoginUseCaseTest#shouldLoginSuccessfully
  ```

### 4.3. Phát hiện quan trọng về Môi trường (ByteBuddy / Mockito Sandbox Issue)
- **Vấn đề quan sát được**: Khi chạy test JUnit/Mockito mặc định trong sandbox agent, ByteBuddy Inline Mock Maker cố gắng tự gắn agent (`self-attach`) qua process ngoài và thất bại (`IllegalStateException: Could not self-attach to current VM using external process`).
- **Giải pháp đã kiểm chứng thành công (100% PASS)**:
  - Khi thực thi lệnh Maven test với quyền hệ thống (`BypassSandbox: true`), ByteBuddy gắn thành công và toàn bộ test case chạy bình thường:
    `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Total time: 6.334 s`.
  - Hoặc để tránh hoàn toàn cơ chế self-attach trên Java 21, có thể cấu hình file `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` chứa nội dung `mock-maker-subclass`.

### 4.4. Các phong cách viết Test hiện có trong Codebase
1. **Pure Unit Test (Tầng Application UseCase)**:
   - Đại diện: `LoginUseCaseTest.java`, `LogoutUseCaseTest.java`, `RegisterUseCaseTest.java`.
   - Đặc điểm:
     - Không tải Spring Context (`@SpringBootTest` không được dùng cho usecase test), giúp test chạy siêu tốc (< 1 giây).
     - Khởi tạo UseCase bằng constructor thủ công trong `@BeforeEach` với các dependency giả lập bằng `mock(Interface.class)`.
     - Dùng `when(...).thenReturn(...)`, `when(...).thenThrow(...)`, và `verify(...)`.
   - **-> ĐÂY LÀ PHONG CÁCH BẮT BUỘC SỬ DỤNG CHO 4 ACCEPTANCE CRITERIA TEST CLASS.**

2. **Standalone MockMvc Controller Test (Tầng Presentation)**:
   - Đại diện: `AuthenticationControllerTest.java`.
   - Đặc điểm: `@ExtendWith(MockitoExtension.class)`, dựng `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(exceptionHandler).build()`.
   - Kiểm tra HTTP status, headers, JSON body, cookies mà không cần khởi động Tomcat server.

3. **Integration Test (Tầng Infrastructure / Filter)**:
   - Đại diện: `RateLimitFilterIntegrationTest.java`.
   - Đặc điểm: Sử dụng Testcontainers (`@Testcontainers`) để khởi chạy container thực (Redis 7.0-alpine).

### 4.5. Cơ chế Mocking Database, Repository và Cloudinary
1. **Mocking Database / Repositories**:
   - Sử dụng Mockito mock trực tiếp Spring Data JPA repositories:
     ```java
     JpaServiceRepository serviceRepository = mock(JpaServiceRepository.class);
     JpaServiceImageRepository imageRepository = mock(JpaServiceImageRepository.class);
     JpaServiceSafetyDocumentRepository safetyDocRepository = mock(JpaServiceSafetyDocumentRepository.class);
     JpaCategoryRepository categoryRepository = mock(JpaCategoryRepository.class);
     ```
2. **Mocking Cloudinary / File Storage**:
   - Tuân thủ Dependency Inversion của Clean Architecture: Tầng Application không gọi trực tiếp thư viện Cloudinary SDK mà thông qua Port interface:
     ```java
     public interface FileStoragePort {
         String uploadImage(MultipartFile file, String folder);
         String uploadDocument(MultipartFile file, String folder);
         void deleteFile(String fileUrlOrPublicId);
     }
     ```
   - Trong UseCase test, mock `FileStoragePort`:
     ```java
     FileStoragePort fileStoragePort = mock(FileStoragePort.class);
     when(fileStoragePort.uploadImage(any(), anyString()))
         .thenReturn("https://res.cloudinary.com/danasea/image/upload/sample.jpg");
     ```

---

## 5. Đặc tả & Cấu trúc chi tiết cho 4 Test Suite theo Acceptance Criteria (Mục tiêu 4)

### 5.1. `UploadServiceImageUseCaseTest`
- **Mục tiêu**: Kiểm tra nghiệp vụ tải lên ảnh cho dịch vụ.
- **Dependencies**: `JpaServiceRepository`, `JpaServiceImageRepository`, `FileStoragePort`.
- **Hằng số nghiệp vụ**: Số ảnh tối đa cho 1 service = 10 (`MAX_SERVICE_IMAGES = 10`), định dạng cho phép: JPEG, PNG, WEBP.
- **Các Test Cases cần có**:
  1. `upload_success_shouldAutoIncrementSortOrder`:
     - Input: Dịch vụ đang có 2 ảnh (sort_order lần lượt là 1, 2). File ảnh hợp lệ `image.jpg`.
     - Output: Ảnh mới được lưu với `sort_order = 3`. Trả về đối tượng `ServiceImage` với đúng `serviceId`, `url`, `sortOrder`.
  2. `upload_whenExceedsMaxImages_shouldThrowException`:
     - Input: Dịch vụ đã có sẵn 10 ảnh (`countByServiceId` trả về 10).
     - Output: Ném ngoại lệ `MaxServiceImagesExceededException` (mã HTTP 400). `fileStoragePort.uploadImage` không được gọi.
  3. `upload_whenInvalidFileFormat_shouldThrowBadRequest`:
     - Input: File có contentType `application/pdf` hoặc đuôi file `.exe`/`.txt`.
     - Output: Ném ngoại lệ `InvalidFileFormatException` (mã HTTP 400).
  4. `upload_whenServiceNotFound_shouldThrowNotFound`:
     - Input: `serviceId` không tồn tại trong database.
     - Output: Ném ngoại lệ `ServiceNotFoundException` (mã HTTP 404).
  5. `upload_whenNotServiceOwner_shouldThrowForbidden`:
     - Input: Service thuộc về `vendorId_A`, nhưng request do `vendorId_B` thực hiện.
     - Output: Ném ngoại lệ `UnauthorizedServiceAccessException` (mã HTTP 403).

### 5.2. `ReorderServiceImagesUseCaseTest`
- **Mục tiêu**: Kiểm tra nghiệp vụ sắp xếp lại thứ tự ảnh hàng loạt (`PATCH /api/vendor/services/{id}/images/reorder`).
- **Dependencies**: `JpaServiceRepository`, `JpaServiceImageRepository`.
- **Các Test Cases cần có**:
  1. `reorder_success_shouldUpdateSortOrdersSequentially`:
     - Input: Service có 3 ảnh [imgA (sort 1), imgB (sort 2), imgC (sort 3)].
     - Yêu cầu đổi thứ tự: danh sách ID mới là [imgC, imgA, imgB].
     - Output: imgC có `sortOrder = 1`, imgA có `sortOrder = 2`, imgB có `sortOrder = 3`. `saveAll` được gọi.
  2. `reorder_whenImageBelongsToDifferentService_shouldThrowForbiddenOrNotFound`:
     - Input: Danh sách imageIds truyền vào chứa 1 ID thuộc về `serviceId` khác (hoặc ID không tồn tại).
     - Output: Ném ngoại lệ `InvalidImageReorderException` hoặc `ForbiddenServiceAccessException` (HTTP 403 / 404).
  3. `reorder_whenNotServiceOwner_shouldThrowForbidden`:
     - Input: Dịch vụ của vendor khác.
     - Output: Ném ngoại lệ HTTP 403.
  4. `reorder_whenIncompleteImageList_shouldThrowBadRequest`:
     - Input: Danh sách ID truyền vào thiếu ảnh hoặc có ID trùng lặp so với các ảnh hiện tại của service.
     - Output: Ném ngoại lệ HTTP 400.

### 5.3. `UploadSafetyDocumentUseCaseTest`
- **Mục tiêu**: Kiểm tra nghiệp vụ upload tài liệu an toàn cho dịch vụ (`POST /api/vendor/services/{id}/safety-documents`).
- **Dependencies**: `JpaServiceRepository`, `JpaServiceSafetyDocumentRepository`, `FileStoragePort`.
- **Trạng thái ban đầu**: Mọi tài liệu mới tải lên đều có `status = DocStatus.PENDING`, `reviewedBy = null`, `reviewedAt = null`.
- **Các Test Cases cần có**:
  1. `uploadSafetyDocument_success`:
     - Input: File PDF chứng chỉ an toàn hợp lệ (`application/pdf`).
     - Output: Lưu vào database với `status = DocStatus.PENDING`.
  2. `uploadSafetyDocument_whenInvalidFileType_shouldThrowBadRequest`:
     - Input: File định dạng không hợp lệ (không phải PDF, JPG, PNG).
     - Output: Ném ngoại lệ HTTP 400.
  3. `uploadSafetyDocument_whenNotServiceOwner_shouldThrowForbidden`:
     - Input: Vendor không sở hữu service.
     - Output: Ném ngoại lệ HTTP 403.

### 5.4. `ApproveSafetyDocumentUseCaseTest` (kèm logic Publish)
- **Mục tiêu**: Kiểm tra quy trình Admin duyệt/từ chối tài liệu và kiểm tra logic publish service dựa trên trạng thái chứng chỉ an toàn và cờ `requires_safety_cert`.
- **Dependencies**: `JpaServiceSafetyDocumentRepository`, `JpaServiceRepository`, `JpaCategoryRepository`.
- **Các Test Cases cần có**:
  1. `approveSafetyDocument_success`:
     - Input: Admin duyệt tài liệu PENDING.
     - Output: Cập nhật `status = DocStatus.APPROVED`, gán `reviewedBy = adminId`, `reviewedAt = now()`.
  2. `rejectSafetyDocument_success`:
     - Input: Admin từ chối tài liệu kèm lý do từ chối.
     - Output: Cập nhật `status = DocStatus.REJECTED`.
  3. `publishService_whenHighRiskCategory_withApprovedDocument_shouldSucceed`:
     - Input: Category có `requiresSafetyCert = true`. Dịch vụ có 1 tài liệu ở trạng thái `DocStatus.APPROVED`.
     - Output: Service chuyển trạng thái thành `PUBLISHED` thành công.
  4. `publishService_whenHighRiskCategory_withoutApprovedDocument_shouldThrowException`:
     - Input: Category có `requiresSafetyCert = true`. Dịch vụ chỉ có tài liệu `PENDING` hoặc `REJECTED` (hoặc không có tài liệu nào).
     - Output: Ném ngoại lệ `SafetyDocumentRequiredException` (HTTP 400 / 409). Trạng thái service KHÔNG bị đổi thành `PUBLISHED`.
  5. `publishService_whenWeatherSensitive_withoutApprovedDocument_shouldThrowException`:
     - Input: Dịch vụ có `weatherSensitive = true` (dù category `requiresSafetyCert = false`). Chưa có tài liệu nào được `APPROVED`.
     - Output: Ném ngoại lệ `SafetyDocumentRequiredException`.
  6. `publishService_whenNormalService_shouldSucceedWithoutSafetyDocument`:
     - Input: Service có `weatherSensitive = false` và Category có `requiresSafetyCert = false`.
     - Output: Publish thành công mà không yêu cầu tài liệu an toàn.

---

## 6. Đề xuất Danh sách File Cần tạo & Sửa đổi cho Implementation

| STT | File Path | Thao tác | Mục đích |
|---|---|---|---|
| 1 | `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Category.java` | Sửa | Thêm `requiresSafetyCert` (Boolean) |
| 2 | `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/CategoryJpaEntity.java` | Sửa | Thêm cột `requires_safety_cert` |
| 3 | `backend/src/main/java/com/danasea/backend/modules/service/domain/exception/SafetyDocumentRequiredException.java` | Tạo mới | Ngoại lệ khi publish thiếu tài liệu duyệt |
| 4 | `backend/src/main/java/com/danasea/backend/modules/service/domain/exception/MaxServiceImagesExceededException.java` | Tạo mới | Ngoại lệ khi vượt quá 10 ảnh |
| 5 | `backend/src/main/java/com/danasea/backend/modules/service/domain/exception/InvalidFileFormatException.java` | Tạo mới | Ngoại lệ định dạng file |
| 6 | `backend/src/main/java/com/danasea/backend/modules/service/domain/exception/UnauthorizedServiceAccessException.java` | Tạo mới | Ngoại lệ bảo vệ quyền Vendor |
| 7 | `backend/src/main/java/com/danasea/backend/modules/service/application/port/FileStoragePort.java` | Tạo mới | Port trừu tượng hóa Cloudinary |
| 8 | `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/storage/CloudinaryStorageAdapter.java` | Tạo mới | Adapter thực thi upload Cloudinary |
| 9 | `backend/src/main/java/com/danasea/backend/modules/service/application/usecase/UploadServiceImageUseCase.java` | Tạo mới | UseCase upload ảnh tự tăng sort_order |
| 10 | `backend/src/main/java/com/danasea/backend/modules/service/application/usecase/ReorderServiceImagesUseCase.java` | Tạo mới | UseCase sắp xếp ảnh hàng loạt |
| 11 | `backend/src/main/java/com/danasea/backend/modules/service/application/usecase/UploadSafetyDocumentUseCase.java` | Tạo mới | UseCase upload tài liệu an toàn |
| 12 | `backend/src/main/java/com/danasea/backend/modules/service/application/usecase/ApproveSafetyDocumentUseCase.java` | Tạo mới | UseCase duyệt tài liệu an toàn & publish guard |
| 13 | `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceImageRepository.java` | Sửa | Thêm các query tìm kiếm, đếm ảnh |
| 14 | `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceSafetyDocumentRepository.java` | Sửa | Thêm method tìm kiếm và kiểm tra `existsByServiceIdAndStatus` |
| 15 | `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/UploadServiceImageUseCaseTest.java` | Tạo mới | Unit Test suite AC 1 |
| 16 | `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/ReorderServiceImagesUseCaseTest.java` | Tạo mới | Unit Test suite AC 2 |
| 17 | `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/UploadSafetyDocumentUseCaseTest.java` | Tạo mới | Unit Test suite AC 3 |
| 18 | `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/ApproveSafetyDocumentUseCaseTest.java` | Tạo mới | Unit Test suite AC 4 |
