# Project: Categories Module API & Unit Tests

## Architecture
Dự án áp dụng **Modular Clean Architecture** (theo quy chuẩn tại `backend/docs/Clean_Architecture_Rules.md`):
- **Domain Layer** (`com.danasea.backend.modules.service.domain` hoặc `com.danasea.backend.modules.category.domain`):
  - Model: `Category` (kế thừa `BaseDomainModel`: `id`, `name`, `nameEn`, `slug`, `parentId`, `iconUrl`, `isActive`, `createdAt`, `updatedAt`).
  - Ports: `CategoryRepositoryPort` (hoặc `CategoryRepository`), `ServiceQueryPort` (hoặc method kiểm tra active services).
  - Exceptions: `CategoryNotFoundException`, `SlugAlreadyExistsException`, `CategoryHasActiveServicesException`, `CategoryHierarchyLoopException` kế thừa `RuntimeException`.
- **Application Layer** (`...application`):
  - Use Cases:
    - `CreateCategoryUseCase`: tạo category gốc (`parentId == null`) hoặc con, validate parent tồn tại (404), validate unique slug, chống vòng lặp phân cấp.
    - `GetCategoryTreeUseCase`: build cây nhiều tầng không đệ quy vô hạn, hỗ trợ lọc `isActive=true` (cho public) hoặc lấy tất cả (cho admin).
    - `UpdateCategoryUseCase`: cập nhật thông tin category, validate slug, validate parent, chống vòng lặp phân cấp.
    - `DeactivateCategoryUseCase`: soft delete (`isActive = false`), kiểm tra nếu có active service (`status = PUBLISHED`) thì throw `CategoryHasActiveServicesException`.
- **Infrastructure Layer** (`...infrastructure`):
  - Entity: `CategoryJpaEntity` (@Table(name = "categories"), unique constraint trên slug, `parentId`, `isActive`, ...).
  - Persistence: `JpaCategoryRepository` (Spring Data JPA), `CategoryPersistenceAdapter` implementing `CategoryRepositoryPort`.
  - Service Check: `JpaServiceRepository` bổ sung query `boolean existsByCategoryIdAndStatus(UUID categoryId, ServiceStatus status)`.
  - Mapper: `CategoryMapper` (@Component) ánh xạ giữa JpaEntity, Domain Model và DTOs.
- **Presentation Layer** (`...presentation`):
  - DTOs (Java records): `CreateCategoryRequest`, `UpdateCategoryRequest`, `CategoryResponse`, `CategoryTreeResponse`.
  - Controllers:
    - `CategoryController`: `GET /api/categories` (Public tree, `isActive = true`).
    - `AdminCategoryController`: `GET /api/admin/categories` (Admin tree/list), `POST /api/admin/categories`, `PATCH /api/admin/categories/{id}`, `PATCH /api/admin/categories/{id}/deactivate`.
  - Security: `SecurityConfig.java` cho phép `GET /api/categories`, `GET /api/categories/**` permitAll. Admin endpoints bảo vệ qua `@PreAuthorize("hasRole('ADMIN')")` và URL matching.
  - Exception Handling: `@RestControllerAdvice` (hoặc bổ sung vào handler chung) trả về `ErrorResponse(String code, String message)`.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | F1_ENTITY_SCHEMA | Chuẩn hóa CategoryJpaEntity (table "categories", unique slug, fields đầy đủ), Domain model Category, JpaCategoryRepository | M1 | ORIGINAL_REQUEST §R2 |
| 2 | F2_EXCEPTIONS | Tạo Custom Exceptions (CategoryNotFoundException, SlugAlreadyExistsException, CategoryHasActiveServicesException, CategoryHierarchyLoopException) và Handler trả về ErrorResponse chuẩn | M1 | ORIGINAL_REQUEST §R1, R2 |
| 3 | F3_CREATE_USECASE | CreateCategoryUseCase: tạo category gốc (parent_id=null), category con (parent_id hợp lệ), 404 khi parent không tồn tại, kiểm tra trùng slug, chống vòng lặp phân cấp | M2 | ORIGINAL_REQUEST §R1, AC |
| 4 | F4_GET_TREE_USECASE | GetCategoryTreeUseCase: xây dựng cây phân cấp cha-con nhiều tầng không đệ quy vô hạn. Public chỉ lấy is_active=true, Admin xem tất cả | M2 | ORIGINAL_REQUEST §R1, AC |
| 5 | F5_UPDATE_USECASE | UpdateCategoryUseCase: cập nhật thông tin category (name, name_en, slug, parent_id, icon_url), validate parent, slug, chống loop | M2 | ORIGINAL_REQUEST §R1 |
| 6 | F6_DEACTIVATE_USECASE | DeactivateCategoryUseCase: set is_active=false (soft delete), ném CategoryHasActiveServicesException nếu có service PUBLISHED | M2 | ORIGINAL_REQUEST §R1, AC |
| 7 | F7_CONTROLLERS_SECURITY | Public CategoryController (GET /api/categories), Admin CategoryController, SecurityConfig permitAll & hasRole('ADMIN') | M3 | ORIGINAL_REQUEST §R1 |
| 8 | F8_UNIT_TESTS | 8 Unit Tests bắt buộc: CreateCategoryUseCaseTest (5 cases), GetCategoryTreeUseCaseTest (2 cases), DeactivateCategoryUseCaseTest (1 case) | M4 | ORIGINAL_REQUEST §Acceptance Criteria |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Domain & Persistence Foundation | Entity, DTO, Repository, Domain Exceptions, Exception Handler | none | DONE (worker_2, verified by reviewer_3/4, auditor_2) |
| M2 | Use Cases & Business Logic | CreateCategoryUseCase, GetCategoryTreeUseCase, UpdateCategoryUseCase, DeactivateCategoryUseCase, Cycle detection, Active services check | M1 | DONE (worker_2, stress-tested by challenger_3/4) |
| M3 | Presentation & Security Configuration | CategoryController, AdminCategoryController, SecurityConfig permitAll/hasRole, Bean wiring | M2 | DONE (worker_2, verified by reviewer_4) |
| M4 | Unit Tests & Verification | CreateCategoryUseCaseTest (5 cases), GetCategoryTreeUseCaseTest (2 cases), DeactivateCategoryUseCaseTest (1 case) pass 100% | M2, M3 | DONE (56/56 category tests pass, 86/86 project tests pass, CLEAN audit) |

## Interface Contracts

### CategoryRepositoryPort
```java
public interface CategoryRepositoryPort {
    Category save(Category category);
    Optional<Category> findById(UUID id);
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsById(UUID id);
    List<Category> findAll();
    List<Category> findAllActive();
}
```

### ActiveServiceCheckPort / ServiceQueryPort
```java
public interface ActiveServiceCheckPort {
    boolean hasActiveServices(UUID categoryId);
}
```

### Presentation DTOs
```java
public record CreateCategoryRequest(
    @NotBlank String name,
    String nameEn,
    @NotBlank String slug,
    UUID parentId,
    String iconUrl
) {}

public record UpdateCategoryRequest(
    String name,
    String nameEn,
    String slug,
    UUID parentId,
    String iconUrl
) {}

public record CategoryResponse(
    UUID id,
    String name,
    String nameEn,
    String slug,
    UUID parentId,
    String iconUrl,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}

public record CategoryTreeResponse(
    UUID id,
    String name,
    String nameEn,
    String slug,
    UUID parentId,
    String iconUrl,
    Boolean isActive,
    List<CategoryTreeResponse> children
) {}
```

## Code Layout
- Root package: `backend/src/main/java/com/danasea/backend/modules/service/` (hoặc `category/` song song)
- Domain:
  - `domain/models/Category.java`
  - `domain/exception/CategoryNotFoundException.java`
  - `domain/exception/SlugAlreadyExistsException.java`
  - `domain/exception/CategoryHasActiveServicesException.java`
  - `domain/exception/CategoryHierarchyLoopException.java`
- Application:
  - `application/port/output/CategoryRepositoryPort.java`
  - `application/port/output/ActiveServiceCheckPort.java`
  - `application/usecase/CreateCategoryUseCase.java`
  - `application/usecase/GetCategoryTreeUseCase.java`
  - `application/usecase/UpdateCategoryUseCase.java`
  - `application/usecase/DeactivateCategoryUseCase.java`
- Infrastructure:
  - `infrastructure/persistence/entities/CategoryJpaEntity.java`
  - `infrastructure/persistence/repositories/JpaCategoryRepository.java`
  - `infrastructure/persistence/repositories/JpaServiceRepository.java`
  - `infrastructure/persistence/adapters/CategoryPersistenceAdapter.java`
  - `infrastructure/mapper/CategoryMapper.java`
- Presentation:
  - `presentation/controller/CategoryController.java`
  - `presentation/controller/AdminCategoryController.java`
  - `presentation/dto/CreateCategoryRequest.java`
  - `presentation/dto/UpdateCategoryRequest.java`
  - `presentation/dto/CategoryResponse.java`
  - `presentation/dto/CategoryTreeResponse.java`
  - `presentation/handler/CategoryExceptionHandler.java`
- Security:
  - `config/SecurityConfig.java`
- Tests:
  - `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/CreateCategoryUseCaseTest.java`
  - `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/GetCategoryTreeUseCaseTest.java`
  - `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/DeactivateCategoryUseCaseTest.java`
