# Báo Cáo Khảo Sát Kiến Trúc Codebase Backend Spring Boot (Categories Module Survey)

**Người thực hiện**: `survey_explorer_1`  
**Thời gian khảo sát**: 2026-09-10  
**Dự án**: DanaSea Backend (`backend/`)  
**Tài liệu tham chiếu gốc**: 
- `backend/docs/Clean_Architecture_Rules.md`
- `backend/docs/DANASEA_Database_Design.docx`
- `.agents/ORIGINAL_REQUEST.md`

---

## 1. Package Naming Conventions & Existing Domain Modules

### 1.1 Cấu trúc Package Gốc
Mã nguồn Spring Boot nằm tại `backend/src/main/java/com/danasea/backend/`. Package gốc là `com.danasea.backend`.

Hệ thống được tổ chức theo mô hình **Modular Clean Architecture** (quy định tại `backend/docs/Clean_Architecture_Rules.md`, Mục 9):

```
com.danasea.backend
├── config/                     # Cấu hình Spring Beans toàn cục, OpenApi, RabbitMQ, Security
├── security/                   # Bounded Context Bảo mật & Xác thực
│   ├── authentication/         # Clean Architecture hoàn chỉnh (domain, application, infrastructure, presentation)
│   └── authorization/          # Quản lý phân quyền & RBAC (AccessDeniedException, AuthorizationHandler)
├── shared/                     # Kernel dùng chung cho toàn bộ hệ thống
│   ├── core/
│   │   ├── domain/models/      # BaseDomainModel.java (id, createdAt, updatedAt)
│   │   └── infrastructure/persistence/entities/ # BaseJpaEntity.java (@MappedSuperclass, UUID, Timestamps)
│   └── presentation/           # ErrorResponse.java (record code, message)
└── modules/                    # Các Domain / Business Modules
    ├── account/                # Quản lý người dùng, refresh token, audit log
    ├── ai/                     # Quản lý hội thoại & tin nhắn AI
    ├── communication/          # Tin nhắn, khiếu nại (dispute)
    ├── operation/              # Vận hành, ca làm, log
    ├── order/                  # Đơn hàng (master order, sub-order)
    ├── service/                # Dịch vụ du lịch biển, danh mục (Category), slot, ảnh, đánh giá
    ├── systemconfig/           # Cấu hình hệ thống chung
    ├── vendor/                 # Nhà cung cấp dịch vụ
    └── weather/                # Dữ liệu & cảnh báo thời tiết
```

### 1.2 Điểm Lưu Ý Về Package (`config` vs `systemconfig`)
Trong thư mục `backend/src/main/java/com/danasea/backend/config/`:
- Các file `ApplicationBeans.java`, `SecurityConfig.java`, và `OpenApiConfig.java` hiện đang khai báo dòng đầu tiên:
  `package com.danasea.backend.modules.systemconfig;`
  mặc dù vị trí tệp nằm trong thư mục `com/danasea/backend/config/`.
- File `RabbitMQConfig.java` thì khai báo đúng: `package com.danasea.backend.config;`.
- **Khuyến nghị**: Khi khai báo các Bean cấu hình cho module Categories, nên khai báo trong cấu hình của chính module hoặc đặt đúng package chuẩn để tránh xung đột package.

---

## 2. Architecture Pattern: Clean Architecture / UseCases

Dự án **KHÔNG** dùng mô hình 3 lớp truyền thống thông thường (Controller -> Service -> Repository) mà áp dụng nghiêm ngặt **Clean Architecture / Hexagonal Architecture** với **Use Cases**, được tài liệu hóa chi tiết tại `backend/docs/Clean_Architecture_Rules.md`.

### 2.1 Chiều Phụ Thuộc (Dependency Direction)
```
Presentation → Application → Domain ← Infrastructure
```
- **Domain Layer (`domain/`)**:
  - Chứa domain models độc lập, kế thừa `BaseDomainModel` (`com.danasea.backend.shared.core.domain.models.BaseDomainModel`).
  - Chứa Domain Exceptions (kế thừa `RuntimeException`).
  - Chứa Domain Events (nếu có).
  - Hoàn toàn độc lập với Spring Boot, Hibernate, JPA hay HTTP.
- **Application Layer (`application/`)**:
  - `usecase/`: Chứa các UseCase độc lập (ví dụ mẫu: `LoginUseCase`, `RegisterUseCase`).
    - Các lớp UseCase là pure Java class, sử dụng constructor injection (thường kèm `@RequiredArgsConstructor`), **không** gắn annotation `@Service` của Spring để giữ tính độc lập.
    - Được instantiate và expose dưới dạng Spring `@Bean` trong class `@Configuration` (xem `ApplicationBeans.java:25-86`).
  - `port/`: Chứa các interface (cổng giao tiếp Inward/Outward) định nghĩa các thao tác dữ liệu mà UseCase cần (ví dụ: `UserAccountPort`, `TokenProvider`).
  - `result/`: Các Java `record` đại diện cho kết quả nghiệp vụ trả về từ UseCase (ví dụ: `LoginResult`).
- **Infrastructure Layer (`infrastructure/`)**:
  - `persistence/entities/`: Các JPA Entity kế thừa `BaseJpaEntity` (`shared.core.infrastructure.persistence.entities.BaseJpaEntity`).
  - `persistence/repositories/`: Spring Data JPA interfaces (kế thừa `JpaRepository<Entity, UUID>`).
  - `persistence/adapter/` hoặc `persistence/`: Các Adapter class triển khai các `port` của Application layer (ví dụ: `UserAccountAdapter implements UserAccountPort`).
  - `mapper/`: Các Spring `@Component` chuyển đổi giữa JPA Entity và Domain Model (ví dụ: `UserMapper.java`).
- **Presentation Layer (`presentation/`)**:
  - Các `@RestController` (ví dụ: `AuthenticationController.java`).
  - `dto/`: Request/Response dạng Java `record` với validation (`@Valid`, `@NotBlank`, `@Size`, v.v.).
  - `@RestControllerAdvice`: Xử lý lỗi theo module (ví dụ: `AuthenticationExceptionHandler.java`).

### 2.2 Quy ước Unit Test
- Các UseCase test (như `LoginUseCaseTest.java`) là **Pure Unit Test**:
  - Dùng JUnit 5 (`@Test`, `@BeforeEach`).
  - Dùng Mockito (`mock(...)`, `when(...)`, `verify(...)`).
  - Khởi tạo trực tiếp instance `new SpecificUseCase(...)` trong `@BeforeEach setUp()`, không nạp Spring Context (`@SpringBootTest` không cần thiết ở tầng này) giúp test chạy siêu nhanh và cô lập.
- Controller test (như `AuthenticationControllerTest.java`):
  - Dùng `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ModuleExceptionHandler()).build()`.

---

## 3. Khảo Sát Các Entities Hiện Có (Category, Service, Quan Hệ & Trạng Thái)

### 3.1 Entity `Category` đã có sẵn trong codebase!
Tại module `modules/service`:
1. **Domain Model**: `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Category.java`:
   ```java
   package com.danasea.backend.modules.service.domain.models;
   
   import java.util.UUID;
   import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
   import lombok.Data;
   import lombok.EqualsAndHashCode;
   
   @Data
   @EqualsAndHashCode(callSuper = true)
   public class Category extends BaseDomainModel {
       private String name;
       private String nameEn;
       private String slug;
       private UUID parentId;
       private String iconUrl;
       private Boolean isActive;
   }
   ```
   *Ghi chú*: Kế thừa `BaseDomainModel` có sẵn: `UUID id`, `OffsetDateTime createdAt`, `OffsetDateTime updatedAt`.

2. **JPA Entity**: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/CategoryJpaEntity.java`:
   ```java
   package com.danasea.backend.modules.service.infrastructure.persistence.entities;
   
   import java.util.UUID;
   import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
   import jakarta.persistence.*;
   import lombok.Getter;
   import lombok.Setter;
   
   @Entity
   @Getter
   @Setter
   @Table(name = "categorys") // Lưu ý: typo "categorys", thiết kế DB trong DOCX ghi là "categories"
   public class CategoryJpaEntity extends BaseJpaEntity {
       private String name;
       private String nameEn;
       private String slug;
       private UUID parentId;
       private String iconUrl;
       private Boolean isActive;
   }
   ```
   *Ghi chú*: Kế thừa `BaseJpaEntity` có sẵn:
   - `@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;`
   - `@CreationTimestamp @Column(name = "created_at", updatable = false) private OffsetDateTime createdAt;`
   - `@UpdateTimestamp @Column(name = "updated_at") private OffsetDateTime updatedAt;`

3. **Repository**: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaCategoryRepository.java`:
   ```java
   package com.danasea.backend.modules.service.infrastructure.persistence.repositories;
   
   import java.util.UUID;
   import com.danasea.backend.modules.service.infrastructure.persistence.entities.CategoryJpaEntity;
   import org.springframework.data.jpa.repository.JpaRepository;
   import org.springframework.stereotype.Repository;
   
   @Repository
   public interface JpaCategoryRepository extends JpaRepository<CategoryJpaEntity, UUID> {
   }
   ```

### 3.2 Entity `Service` và Mối Quan Hệ Với `Category`
1. **Domain Model**: `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Service.java`:
   - Chứa trường: `private UUID categoryId;`
   - Chứa trường: `private ServiceStatus status;`
2. **JPA Entity**: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceJpaEntity.java`:
   ```java
   @Entity
   @Getter
   @Setter
   @Table(name = "services")
   public class ServiceJpaEntity extends BaseJpaEntity {
       private UUID vendorId;
       private UUID categoryId; // Khóa ngoại trỏ tới categories.id
       ...
       @Enumerated(EnumType.STRING)
       private ServiceStatus status;
       ...
   }
   ```
3. **Repository**: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceRepository.java`.

### 3.3 Quy Ước Quan Hệ Foreign Key Trong Toàn Bộ Codebase
- **Đặc thù quan trọng**: Trong toàn bộ codebase DanaSea, các bảng/thực thể **KHÔNG sử dụng quan hệ đối tượng JPA** (`@ManyToOne`, `@OneToMany`, `@JoinColumn`).
- Thay vào đó, toàn bộ quan hệ lưu trữ dưới dạng **trường ID thuần** (`UUID categoryId`, `UUID parentId`, `UUID serviceId`, `UUID userId`, `UUID vendorId`).
- Đây là nguyên tắc phân ranh giới ngữ cảnh (Aggregate Root reference by ID) điển hình của DDD / Clean Architecture.

### 3.4 Định Nghĩa "Active Service" (Dịch Vụ Đang Hoạt Động)
- `ServiceJpaEntity` **không có cột `is_active`**. Thay vào đó, vòng đời của dịch vụ được quản lý qua enum `ServiceStatus`:
  ```java
  package com.danasea.backend.modules.service.domain.models;
  
  public enum ServiceStatus {
      DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, PAUSED
  }
  ```
- Theo tài liệu `backend/docs/DANASEA_Database_Design.docx` (mục 2, dòng 241-243), dịch vụ có trạng thái `PUBLISHED` là dịch vụ đang hoạt động được đăng bán trên sàn.
- Do đó, nghiệp vụ kiểm tra deactivation (`CategoryHasActiveServicesException`) cần kiểm tra:
  Liệu có bất kỳ `ServiceJpaEntity` nào có `categoryId = :categoryId` và `status = ServiceStatus.PUBLISHED` hay không (hoặc mở rộng kiểm tra cả `PAUSED` nếu dịch vụ chỉ tạm dừng nhưng vẫn còn hiệu lực). Có thể tạo phương thức trên repository/port:
  ```java
  boolean existsByCategoryIdAndStatus(UUID categoryId, ServiceStatus status);
  ```

---

## 4. Quy Ước Xử Lý Ngoại Lệ (Exception Handling Conventions)

### 4.1 Cơ chế hiện có trong Codebase
1. **Domain Exceptions**:
   - Vị trí: Đặt trong package `domain/exception/` của module (ví dụ: `com.danasea.backend.modules.service.domain.exception`).
   - Kế thừa: Kế thừa trực tiếp `RuntimeException`.
   - Cung cấp message rõ ràng hoặc constructor mặc định.
2. **Xử lý tại Controller Advice**:
   - Vị trí: Đặt trong package `presentation/` của module hoặc shared.
   - Annotation: `@RestControllerAdvice`.
   - Tham khảo: `AuthenticationExceptionHandler.java` (`security/authentication/presentation/`) và `AuthorizationHandler.java` (`security/authorization/presentation/`).
3. **Cấu trúc Response Lỗi Chuẩn**:
   - Sử dụng record dùng chung `com.danasea.backend.shared.presentation.ErrorResponse`:
     ```java
     package com.danasea.backend.shared.presentation;
     
     public record ErrorResponse(
             String code,
             String message
     ) {}
     ```
4. **Bảng Map Mã Lỗi & HTTP Status Cho Module Categories**:
   | Ngoại lệ | HTTP Status | ErrorResponse Code | Mô tả |
   |---|---|---|---|
   | `CategoryNotFoundException` | `404 NOT_FOUND` | `CATEGORY_NOT_FOUND` | Category hoặc parentId không tồn tại |
   | `SlugAlreadyExistsException` | `409 CONFLICT` | `SLUG_ALREADY_EXISTS` | Trùng slug với category khác |
   | `CategoryHasActiveServicesException` | `409 CONFLICT` (hoặc 400) | `CATEGORY_HAS_ACTIVE_SERVICES` | Chặn deactivate khi đang có services active |
   | `CategoryHierarchyLoopException` | `400 BAD_REQUEST` | `CATEGORY_HIERARCHY_LOOP` | Chặn vòng lặp cha-con (A → B → A) |
   | `MethodArgumentNotValidException` | `400 BAD_REQUEST` | `INVALID_INPUT` | Lỗi validate bean (@NotBlank, @Size) |

---

## 5. Quy Ước DTO và Mapper

### 5.1 DTO (Data Transfer Objects)
- Toàn bộ DTO trong presentation layer được định nghĩa bằng **Java `record`** (xem ví dụ tại `security/authentication/presentation/dto/`):
  - Request DTOs: ví dụ `CreateCategoryRequest(String name, String nameEn, String slug, UUID parentId, String iconUrl)`, `UpdateCategoryRequest(...)`.
  - Áp dụng các annotation validation của Jakarta: `@NotBlank`, `@Size(max = 100)`, `@Pattern`, v.v.
  - Response DTOs: ví dụ `CategoryResponse(...)`, `CategoryTreeResponse(UUID id, String name, String nameEn, String slug, UUID parentId, String iconUrl, Boolean isActive, List<CategoryTreeResponse> children)`.
- UseCase Results: Java `record` trong `application/result/` (hoặc tái sử dụng DTO nếu cấu trúc trùng khớp).

### 5.2 Mapper
- **Không dùng MapStruct hay ModelMapper**: Trong `pom.xml`, không có dependency `org.mapstruct:mapstruct` và annotation processor của maven compiler chỉ có `lombok`.
- **Quy ước hiện tại**: Viết mapper thủ công dưới dạng Spring Bean `@Component` đặt trong `infrastructure/mapper/` (xem mẫu `UserMapper.java` và `RefreshTokenMapper.java` trong `modules/account/infrastructure/mapper/`).
  - Các hàm điển hình:
    - `public Category toDomain(CategoryJpaEntity entity)`
    - `public CategoryJpaEntity toEntity(Category domain)`
    - `public CategoryResponse toResponse(Category domain)`
    - `public CategoryTreeResponse toTreeResponse(Category domain, List<CategoryTreeResponse> children)`

---

## 6. Đánh Giá Về Phân Quyền & Bảo Mật (Security & Endpoints)

Kiểm tra `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`:
- `@EnableMethodSecurity` đã được kích hoạt trên `SecurityConfig`.
- Hiện tại danh sách `permitAll()` chỉ gồm `/api/auth/**`, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`.
- Để phục vụ yêu cầu:
  1. `GET /api/categories`: Là API Public, cần được thêm vào danh sách `permitAll()` trong `SecurityConfig` (hoặc cấu hình matcher cho phép truy cập nặc danh không cần Token).
  2. Các API `/api/admin/categories/**` (`GET`, `POST`, `PATCH`): Chỉ cho phép Admin (`ROLE_ADMIN`).
     - Có thể dùng `@PreAuthorize("hasRole('ADMIN')")` ngay trên controller methods hoặc class (vì `@EnableMethodSecurity` đã bật).
     - Trong `JwtAuthenticationFilter`, `ROLE_` được map tự động từ danh sách roles của `AuthorizationSubject` (`ROLE_ADMIN`).

---

## 7. Đề Xuất Thiết Kế Kiến Trúc Cho Module Categories

### Lựa Chọn Vị Trí Module
Hiện tại `Category.java`, `CategoryJpaEntity.java`, và `JpaCategoryRepository.java` đã nằm sẵn trong `com.danasea.backend.modules.service`.
Trong tài liệu nghiệp vụ `DANASEA_Database_Design.docx`, Epic-02 gộp chung Service & Category.

Có hai phương án tổ chức:
- **Phương án 1 (Khuyên dùng - Khớp 100% hiện trạng)**: Phát triển Categories ngay trong `com.danasea.backend.modules.service`:
  ```
  com.danasea.backend.modules.service
  ├── domain
  │   ├── models
  │   │   ├── Category.java (đã có)
  │   │   └── Service.java (đã có)
  │   └── exception
  │       ├── CategoryNotFoundException.java
  │       ├── SlugAlreadyExistsException.java
  │       ├── CategoryHasActiveServicesException.java
  │       └── CategoryHierarchyLoopException.java
  ├── application
  │   ├── port
  │   │   ├── CategoryRepositoryPort.java
  │   │   └── ServiceQueryPort.java (hoặc tích hợp trong CategoryRepositoryPort)
  │   └── usecase
  │       ├── CreateCategoryUseCase.java
  │       ├── GetCategoryTreeUseCase.java
  │       ├── DeactivateCategoryUseCase.java
  │       └── UpdateCategoryUseCase.java
  ├── infrastructure
  │   ├── mapper
  │   │   └── CategoryMapper.java
  │   └── persistence
  │       ├── adapter
  │       │   └── CategoryPersistenceAdapter.java
  │       ├── entities
  │       │   ├── CategoryJpaEntity.java (cập nhật @Table(name="categories"))
  │       │   └── ServiceJpaEntity.java (đã có)
  │       └── repositories
  │           ├── JpaCategoryRepository.java (thêm query methods)
  │           └── JpaServiceRepository.java (thêm existsByCategoryIdAndStatus)
  └── presentation
      ├── CategoryController.java (hoặc AdminCategoryController + PublicCategoryController)
      ├── CategoryExceptionHandler.java
      └── dto
          ├── CreateCategoryRequest.java
          ├── UpdateCategoryRequest.java
          ├── CategoryResponse.java
          └── CategoryTreeResponse.java
  ```
- **Phương án 2**: Tách riêng ra module `com.danasea.backend.modules.category`. Khi đó cần di chuyển `Category.java`, `CategoryJpaEntity.java`, `JpaCategoryRepository.java` từ `modules/service` sang `modules/category` để tránh trùng lặp.

---

## 8. Tóm Tắt Các Điểm Trọng Yếu Cho Đội Ngũ Triển Khai (Key Takeaways)

1. **Tuân thủ Clean Architecture**: Use Cases là pure Java classes (`application/usecase`), giao tiếp DB qua Ports (`application/port`), Adapter nằm ở `infrastructure/persistence`.
2. **Khởi tạo Bean**: Đăng ký các UseCase Bean trong class `@Configuration` (tương tự như `ApplicationBeans.java`).
3. **Quan hệ ID**: `ServiceJpaEntity` chứa `categoryId` dạng `UUID`. Không tạo quan hệ `@ManyToOne`.
4. **Kiểm tra Active Service**: Sử dụng `ServiceStatus.PUBLISHED` trên `ServiceJpaEntity` để kiểm tra các dịch vụ đang active của category khi deactivate.
5. **Chống đệ quy vô hạn khi build tree**: Sử dụng cấu trúc `Map<UUID, List<Category>>` theo `parentId` để build cây 1 lần duyệt (O(n)), tránh đệ quy sâu hoặc vòng lặp.
6. **Kiểm tra vòng lặp phân cấp khi gán parent_id**: Duyệt ngược cây cha từ `targetParentId` lên gốc. Nếu gặp chính ID của category đang xét thì ném lỗi `CategoryHierarchyLoopException`.
7. **Sửa lỗi tên bảng**: Sửa `@Table(name = "categorys")` thành `@Table(name = "categories")` trong `CategoryJpaEntity.java` theo đúng tài liệu DB Design.
8. **Unit Test đầy đủ**: Viết unit test cho các Use Case theo đúng danh sách acceptance criteria (`CreateCategoryUseCaseTest`, `GetCategoryTreeUseCaseTest`, `DeactivateCategoryUseCaseTest`) bằng JUnit 5 và Mockito thuần, không phụ thuộc vào Spring context.
