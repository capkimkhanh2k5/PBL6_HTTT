# Progress Log - worker_1

Last visited: 2026-09-10T10:48:40+07:00

- [x] Initialized BRIEFING.md and progress.md
- [ ] Investigate existing service module files and database setup
- [ ] Implement M1: Domain & Persistence Foundation
  - [ ] Update CategoryJpaEntity (@Table(name = "categories"), unique slug, column constraints)
  - [ ] Check/Update Category domain model
  - [ ] Create domain exceptions: CategoryNotFoundException, SlugAlreadyExistsException, CategoryHasActiveServicesException, CategoryHierarchyLoopException
  - [ ] Create CategoryRepositoryPort and ActiveServiceCheckPort
  - [ ] Update JpaCategoryRepository and JpaServiceRepository
  - [ ] Implement CategoryPersistenceAdapter
  - [ ] Implement CategoryMapper (@Component)
- [ ] Implement M2: Use Cases & Business Logic
  - [ ] CreateCategoryUseCase (root, child, 404 on parent not found, slug collision, cycle prevention)
  - [ ] GetCategoryTreeUseCase (safe multi-level tree builder, public active-only vs admin all)
  - [ ] UpdateCategoryUseCase (fields update, slug collision, parent validation, cycle prevention)
  - [ ] DeactivateCategoryUseCase (active service check, soft delete isActive=false)
  - [ ] Register Use Cases as Beans in ApplicationBeans or @Service
- [ ] Implement M3: Presentation & Security Configuration
  - [ ] Create DTOs (CreateCategoryRequest, UpdateCategoryRequest, CategoryResponse, CategoryTreeResponse)
  - [ ] Create CategoryController (GET /api/categories)
  - [ ] Create AdminCategoryController (GET, POST, PATCH, PATCH deactivate)
  - [ ] Create CategoryExceptionHandler (@RestControllerAdvice)
  - [ ] Update SecurityConfig (permitAll /api/categories, /api/categories/**)
- [ ] Implement M4: Unit Tests & Verification
  - [ ] CreateCategoryUseCaseTest (5 test cases)
  - [ ] GetCategoryTreeUseCaseTest (2 test cases)
  - [ ] DeactivateCategoryUseCaseTest (1 test case)
  - [ ] Compile and run tests via mvnw
- [ ] Finalize handoff.md and notify parent
