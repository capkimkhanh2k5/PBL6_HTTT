# Báo Cáo Điều Tra Milestone 2 (Domain & Persistence): Service Images

**Agent**: Explorer 1 (`teamwork_preview_explorer_m2_1`)  
**Mục tiêu**: Điều tra cấu trúc Domain Models, Database Schema, JPA Entities, và Repository Interfaces phục vụ Module Service Images.  
**Thời gian**: 2026-09-10T11:14:30+07:00 (2026-09-10T04:14:30Z)

---

## 1. Observation (Các quan sát trực tiếp)

### 1.1. Hiện trạng các file Domain Models và JPA Entities
Đã kiểm tra cấu trúc mã nguồn trong `backend/src/main/java/com/danasea/backend/modules/service/`:

- **Domain Models** (`com.danasea.backend.modules.service.domain.models`):
  - `ServiceImage.java` (dòng 1-16):
    ```java
    @Data
    @EqualsAndHashCode(callSuper = true)
    public class ServiceImage extends BaseDomainModel {
        private UUID serviceId;
        private String url;
        private Short sortOrder;
    }
    ```
  - `BaseDomainModel.java` (`com.danasea.backend.shared.core.domain.models`, dòng 10-17):
    ```java
    public abstract class BaseDomainModel {
        private UUID id;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }
    ```
  - `Service.java` (dòng 1-36): Extends `BaseDomainModel`, chứa các trường như `UUID vendorId`, `UUID categoryId`, `String name`, `ServiceStatus status`, `Boolean weatherSensitive`, v.v.

- **JPA Entities** (`com.danasea.backend.modules.service.infrastructure.persistence.entities`):
  - `ServiceImageJpaEntity.java` (dòng 1-23):
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
  - `BaseJpaEntity.java` (`com.danasea.backend.shared.core.infrastructure.persistence.entities`, dòng 12-27):
    ```java
    @MappedSuperclass
    @Getter
    @Setter
    public abstract class BaseJpaEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false)
        private OffsetDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at")
        private OffsetDateTime updatedAt;
    }
    ```
  - `ServiceJpaEntity.java` (dòng 1-64):
    - Table name: `@Table(name = "services")`.
    - ID: `UUID id` (kế thừa từ `BaseJpaEntity`).
    - Các trường chính: `UUID vendorId`, `UUID categoryId`, `String name`, `Boolean weatherSensitive`, `@Enumerated(EnumType.STRING) ServiceStatus status`, v.v.

### 1.2. Mối quan hệ giữa ServiceImage và Service
- Cả `ServiceJpaEntity` và `ServiceImageJpaEntity` đều **không** sử dụng các annotation quan hệ ORM trực tiếp như `@ManyToOne`, `@OneToMany`.
- Mối quan hệ được liên kết lỏng (loosely coupled) thông qua khóa ngoại kiểu `UUID`: `ServiceImageJpaEntity` lưu trực tiếp `private UUID serviceId`. Điều này tuân thủ quy tắc Clean Architecture & DDD của dự án.
- Kiểu dữ liệu ID của cả 2 entity là **`UUID`** (không phải `Long`).

### 1.3. Cấu hình Database & Migrations
- File `backend/src/main/resources/application.yml` (dòng 20-23):
  ```yaml
    jpa:
      hibernate:
        ddl-auto: update
  ```
- Không tồn tại Flyway (không có dependency trong `pom.xml`, không có thư mục `src/main/resources/db/migration`).
- Không tồn tại Liquibase (không có dependency trong `pom.xml`).
- Không có file script `.sql` DDL nào cho bảng `service_images` trong toàn bộ repo. Bảng `service_images` được tự động tạo và cập nhật bởi Hibernate dựa trên annotation `@Entity` và `@Table(name = "service_images")` trên PostgreSQL 17.

### 1.4. Trạng thái Repository Interface: `JpaServiceImageRepository`
File `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceImageRepository.java` (dòng 1-27) **ĐÃ TỒN TẠI** và có nội dung đầy đủ như sau:
```java
@Repository
public interface JpaServiceImageRepository extends JpaRepository<ServiceImageJpaEntity, UUID> {

    long countByServiceId(UUID serviceId);

    List<ServiceImageJpaEntity> findByServiceIdOrderBySortOrderAsc(UUID serviceId);

    @Query("SELECT MAX(i.sortOrder) FROM ServiceImageJpaEntity i WHERE i.serviceId = :serviceId")
    Optional<Short> findMaxSortOrderByServiceId(@Param("serviceId") UUID serviceId);

    Optional<ServiceImageJpaEntity> findByIdAndServiceId(UUID id, UUID serviceId);

    void deleteByIdAndServiceId(UUID id, UUID serviceId);
}
```
Cả 5 query methods được quy định trong `PROJECT.md` (dòng 71-76) **đều đã được khai báo chính xác 100%**:
1. `countByServiceId(UUID serviceId)`: Đã có.
2. `findByServiceIdOrderBySortOrderAsc(UUID serviceId)`: Đã có.
3. `findMaxSortOrderByServiceId(UUID serviceId)`: Đã có với `@Query` JPQL chuẩn xác.
4. `findByIdAndServiceId(UUID id, UUID serviceId)`: Đã có.
5. `deleteByIdAndServiceId(UUID id, UUID serviceId)`: Đã có.

### 1.5. Hiện trạng Use Cases và Test Suites Milestone 2
- Use cases hiện tại:
  - `UploadServiceImageUseCase.java`: Đã có skeleton, method `execute` ném `UnsupportedOperationException("Acceptance Criteria: Pending implementation by Milestone 2 Worker")`.
  - `ReorderServiceImagesUseCase.java`: Đã có skeleton, method `execute` ném `UnsupportedOperationException("Acceptance Criteria: Pending implementation by Milestone 2 Worker")`.
  - `DeleteServiceImageUseCase.java`: **Chưa tồn tại**, cần được tạo mới ở M2.
  - `VendorServiceImageController.java`: **Chưa tồn tại**, cần được tạo mới ở M2.
- Test suites:
  - `UploadServiceImageUseCaseTest.java` (207 dòng, 7 test cases AC1.1 -> AC1.7).
  - `ReorderServiceImagesUseCaseTest.java` (199 dòng, 6 test cases AC2.1 -> AC2.6).
  - Kết quả chạy test: `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=UploadServiceImageUseCaseTest,ReorderServiceImagesUseCaseTest` thất bại đúng 13/13 test cases do exception chờ implement.

---

## 2. Logic Chain (Chuỗi suy luận)

1. **Từ quan sát 1.1 & 1.2**:
   - `BaseJpaEntity` định nghĩa `@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;`. Cả `ServiceJpaEntity` và `ServiceImageJpaEntity` đều kế thừa từ lớp này.
   - Suy ra: Toàn bộ thao tác CRUD và khóa chính, khóa ngoại đối với `Service` và `ServiceImage` phải dùng kiểu `UUID`.
   - Các trường cột trong database theo Hibernate naming strategy mặc định:
     - `service_images`: `id` (uuid), `service_id` (uuid), `url` (varchar), `sort_order` (int2/smallint), `created_at` (timestamptz), `updated_at` (timestamptz).
     - `services`: `id` (uuid), `vendor_id` (uuid), `category_id` (uuid), `name` (varchar), v.v.

2. **Từ quan sát 1.3**:
   - Dự án dùng `spring.jpa.hibernate.ddl-auto: update`, không dùng Flyway hay Liquibase.
   - Suy ra: Không cần tạo migration script SQL thủ công. Khi khởi động ứng dụng, Hibernate sẽ tự tạo bảng `service_images` với các cột tương ứng từ `ServiceImageJpaEntity`.

3. **Từ quan sát 1.4**:
   - `JpaServiceImageRepository` đã định nghĩa đầy đủ cả 5 query methods đúng theo `PROJECT.md`.
   - Suy ra: Worker Milestone 2 **không cần bổ sung query method nào** vào `JpaServiceImageRepository`. Interface này đã sẵn sàng để các use cases inject và sử dụng ngay lập tức.

4. **Từ quan sát 1.5**:
   - Các bài test unit trong `UploadServiceImageUseCaseTest` và `ReorderServiceImagesUseCaseTest` đã được viết rất chi tiết, mô tả chính xác từng nghiệp vụ:
     - Giới hạn tối đa 10 ảnh (`MaxImagesExceededException`).
     - Tự động gán `sortOrder = 1` nếu chưa có ảnh, hoặc `max(sortOrder) + 1` nếu đã có.
     - Validate file ảnh: không rỗng, MIME types cho phép (`InvalidFileTypeException`).
     - Kiểm tra quyền sở hữu service (`UnauthorizedServiceAccessException`) khi `!service.getVendorId().equals(vendorId)` (Chống lỗ hổng IDOR).
     - Kiểm tra tồn tại service (`ServiceNotFoundException`).
     - Reorder ảnh: Kiểm tra danh sách ID có thuộc service không, số lượng ID có khớp với số lượng ảnh hiện có không, có trùng lặp ID không.
   - Suy ra: Worker cho Milestone 2 chỉ cần tập trung hoàn thiện logic bên trong `UploadServiceImageUseCase`, `ReorderServiceImagesUseCase`, bổ sung `DeleteServiceImageUseCase`, và viết `VendorServiceImageController` để hoàn tất toàn bộ Milestone 2.

---

## 3. Caveats (Lưu ý và ngoại lệ)

1. **Transaction với derived delete query**:
   - Phương thức `deleteByIdAndServiceId(UUID id, UUID serviceId)` trong `JpaServiceImageRepository` là query xóa của Spring Data JPA. Nếu gọi phương thức này, Spring yêu cầu phải có một Transaction đang hoạt động (`@Transactional` trên Use Case hoặc Service).
   - Ngoài ra, trong `DeleteServiceImageUseCase`, khuyến nghị tìm entity trước bằng `findByIdAndServiceId` để lấy được `url` (nhằm gọi `fileStoragePort.deleteFile(url)` xóa ảnh trên Cloudinary), sau đó gọi `serviceImageRepository.delete(entity)` hoặc `deleteById(id)`. Như vậy vừa đảm bảo tính toàn vẹn dữ liệu, vừa xóa được tài nguyên trên Cloudinary.

2. **Chạy Maven trên môi trường local**:
   - Lệnh `mvn` chưa được thêm vào biến môi trường toàn cục `PATH`.
   - Cần chạy `./mvnw` từ thư mục `backend/` và chỉ định `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`.

3. **Kiểu dữ liệu `sortOrder`**:
   - Trong `ServiceImageJpaEntity`, trường `sortOrder` có kiểu `Short` (`java.lang.Short`), khớp với kiểu `Optional<Short>` trong `findMaxSortOrderByServiceId`. Khi tính toán tăng thứ tự: `(short) (currentMax + 1)`.

---

## 4. Conclusion (Kết luận)

1. **Domain & Persistence Layer cho Service Images đã hoàn thiện 100% về mặt định nghĩa**:
   - `ServiceImage` (domain model) và `ServiceImageJpaEntity` (persistence entity) đã sẵn sàng, sử dụng `UUID` cho khóa chính và khóa ngoại.
   - `JpaServiceImageRepository` đã có đủ 5 method theo hợp đồng `PROJECT.md`.
   - Database schema được đồng bộ tự động qua Hibernate DDL auto-update.
2. **Nhiệm vụ cho Milestone 2 Worker**:
   - Triển khai logic trong `UploadServiceImageUseCase.java` (làm pass 7 test cases).
   - Triển khai logic trong `ReorderServiceImagesUseCase.java` (làm pass 6 test cases).
   - Tạo mới `DeleteServiceImageUseCase.java` (bao gồm xóa DB và xóa trên Cloudinary qua `fileStoragePort`).
   - Tạo mới Presentation Controller `VendorServiceImageController.java` với 3 endpoints:
     - `POST /api/vendor/services/{id}/images`
     - `DELETE /api/vendor/services/{id}/images/{imageId}`
     - `PATCH /api/vendor/services/{id}/images/reorder`

---

## 5. Verification Method (Phương pháp kiểm chứng độc lập)

Bất kỳ agent nào nhận bàn giao có thể kiểm chứng lại toàn bộ quan sát trên bằng các bước sau:

1. **Kiểm tra code các entity và repository**:
   ```bash
   cat backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceImageJpaEntity.java
   cat backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceImageRepository.java
   ```

2. **Kiểm tra cấu hình Hibernate ddl-auto**:
   ```bash
   grep -A 5 "jpa:" backend/src/main/resources/application.yml
   ```

3. **Biên dịch toàn bộ mã nguồn và test**:
   ```bash
   cd backend
   JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test-compile
   ```
   *Kết quả mong đợi*: `BUILD SUCCESS`.

4. **Kiểm tra trạng thái test suites của Milestone 2**:
   ```bash
   cd backend
   JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=UploadServiceImageUseCaseTest,ReorderServiceImagesUseCaseTest
   ```
   *Kết quả mong đợi*: Cả 13 bài test thất bại với `UnsupportedOperationException: Acceptance Criteria: Pending implementation by Milestone 2 Worker`.
