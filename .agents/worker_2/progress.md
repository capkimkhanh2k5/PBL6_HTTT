# Progress Log - worker_2

Last visited: 2026-09-10T10:56:00+07:00

- [x] Initialized BRIEFING.md and progress.md
- [x] Implement M1: Domain & Persistence Foundation
  - [x] Check/Update Category domain model (added SuperBuilder, NoArgsConstructor, AllArgsConstructor)
  - [x] Update CategoryJpaEntity (@Table(name = "categories"), unique slug, column constraints)
  - [x] Create domain exceptions: CategoryNotFoundException, SlugAlreadyExistsException, CategoryHasActiveServicesException, CategoryHierarchyLoopException
  - [x] Create CategoryRepositoryPort and ActiveServiceCheckPort
  - [x] Update JpaCategoryRepository and JpaServiceRepository
  - [x] Implement CategoryPersistenceAdapter
  - [x] Implement CategoryMapper (@Component)
- [x] Implement M2: Use Cases & Business Logic
  - [x] CreateCategoryUseCase (root, child, 404 on parent not found, slug collision, cycle prevention)
  - [x] GetCategoryTreeUseCase (safe non-recursive multi-level tree builder, public active-only vs admin all)
  - [x] UpdateCategoryUseCase (fields update, slug collision, parent validation, cycle prevention)
  - [x] DeactivateCategoryUseCase (active service check, soft delete isActive=false)
  - [x] Register Use Cases as Beans with @Service
- [x] Implement M3: Presentation & Security Configuration
  - [x] Create DTOs (CreateCategoryRequest, UpdateCategoryRequest, CategoryResponse, CategoryTreeResponse)
  - [x] Create CategoryController (GET /api/categories)
  - [x] Create AdminCategoryController (GET, POST, PATCH, PATCH deactivate)
  - [x] Create CategoryExceptionHandler (@RestControllerAdvice)
  - [x] Update SecurityConfig (permitAll /api/categories, /api/categories/**, hasRole ADMIN for /api/admin/**)
- [x] Implement M4: Unit Tests & Verification
  - [x] CreateCategoryUseCaseTest (5 test cases)
  - [x] GetCategoryTreeUseCaseTest (2 test cases)
  - [x] DeactivateCategoryUseCaseTest (3 test cases)
  - [x] Compile and run tests via mvnw (10/10 passed, full suite 40/40 passed)
- [ ] Finalize handoff.md and notify parent
