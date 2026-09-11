# Task Assignment for worker_1

**Role**: Full-Stack Backend Implementer & Test Specialist
**Working Directory**: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/worker_1
**Original Request**: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/ORIGINAL_REQUEST.md
**Project Plan**: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/PROJECT.md
**Explorer Findings**:
- /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/survey_explorer_1/handoff.md
- /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/survey_explorer_2/handoff.md
- /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/survey_explorer_3/handoff.md

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Objective
Implement the complete Categories module (M1, M2, M3, M4) according to Modular Clean Architecture in `backend/` and verify all 8 required Unit Tests pass 100%.

## Scope of Work
1. **Domain & Persistence (M1)**:
   - Fix `CategoryJpaEntity.java`: change `@Table(name = "categorys")` to `@Table(name = "categories")`, add unique constraint on `slug`, ensure fields match design.
   - Domain model `Category.java`: verify/update fields.
   - Domain Exceptions:
     - `CategoryNotFoundException` (HTTP 404)
     - `SlugAlreadyExistsException` (HTTP 409)
     - `CategoryHasActiveServicesException` (HTTP 400/409)
     - `CategoryHierarchyLoopException` (HTTP 400)
   - Ports & Adapters:
     - `CategoryRepositoryPort` interface in `application/port/output/`
     - `ActiveServiceCheckPort` interface in `application/port/output/`
     - `JpaCategoryRepository` queries (findBySlug, existsBySlug, etc.)
     - `JpaServiceRepository`: add query `boolean existsByCategoryIdAndStatus(UUID categoryId, ServiceStatus status)`
     - `CategoryPersistenceAdapter` implementing `CategoryRepositoryPort` and `ActiveServiceCheckPort`
     - `CategoryMapper` (@Component) mapping between JpaEntity, Domain, and DTOs.
2. **Use Cases & Business Logic (M2)**:
   - `CreateCategoryUseCase`:
     - Root category creation (`parentId == null`)
     - Child category creation with valid `parentId`
     - Throw `CategoryNotFoundException` if `parentId` does not exist
     - Throw `SlugAlreadyExistsException` if slug exists
     - Anti-cycle check: if setting parent, ensure no circular hierarchy (A -> B -> A)
   - `GetCategoryTreeUseCase`:
     - Build hierarchical cha-con tree without infinite recursion (handle multi-level, e.g. root -> child -> grandchild)
     - Filter `isActive == true` for public
     - Return all categories (active & inactive) for admin
   - `UpdateCategoryUseCase`:
     - Update name, nameEn, slug, parentId, iconUrl
     - Validate existence, unique slug (excluding self), and prevent circular hierarchy
   - `DeactivateCategoryUseCase`:
     - Check if category has active services (`ServiceStatus.PUBLISHED`) via `ActiveServiceCheckPort`.
     - Throw `CategoryHasActiveServicesException` if active services exist
     - Soft delete: set `isActive = false`
3. **Presentation & Security (M3)**:
   - DTOs: `CreateCategoryRequest`, `UpdateCategoryRequest`, `CategoryResponse`, `CategoryTreeResponse`
   - Controllers:
     - `CategoryController`: `GET /api/categories` (public tree)
     - `AdminCategoryController`:
       - `GET /api/admin/categories` (admin list/tree)
       - `POST /api/admin/categories` (create)
       - `PATCH /api/admin/categories/{id}` (update)
       - `PATCH /api/admin/categories/{id}/deactivate` (deactivate)
       - Annotated with `@PreAuthorize("hasRole('ADMIN')")`
   - Security:
     - Update `SecurityConfig.java`: permit `GET /api/categories` and `GET /api/categories/**` for public.
   - Bean Registration:
     - Annotate use cases with Spring `@Service` / `@Component`, or declare `@Bean` in a configuration class (e.g. `CategoryConfig` or `ApplicationBeans`).
   - Exception Handler:
     - `CategoryExceptionHandler` (`@RestControllerAdvice`) handling the 4 custom exceptions and returning `ErrorResponse(code, message)`.
4. **Unit Tests Implementation (M4)**:
   - Create the 3 test classes with Mockito mocks following project conventions:
     - `CreateCategoryUseCaseTest`:
       1. `shouldCreateRootCategorySuccessfully_WhenParentIdIsNull`
       2. `shouldCreateChildCategorySuccessfully_WhenParentIdIsValid`
       3. `shouldThrowCategoryNotFoundException_WhenParentIdDoesNotExist`
       4. `shouldThrowSlugAlreadyExistsException_WhenSlugAlreadyExists`
       5. `shouldThrowCategoryHierarchyLoopException_WhenSettingChildAsParent`
     - `GetCategoryTreeUseCaseTest`:
       6. `shouldReturnMultiLevelTreeStructure`
       7. `shouldFilterInactiveCategoriesForPublic_ButIncludeThemForAdmin`
     - `DeactivateCategoryUseCaseTest`:
       8. `shouldThrowCategoryHasActiveServicesException_WhenCategoryHasActiveServices`
5. **Verification**:
   - Run compilation and tests:
     ```bash
     cd /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/backend
     export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
     export PATH=$JAVA_HOME/bin:$PATH
     ./mvnw test-compile
     ./mvnw test -Dtest="*CategoryUseCaseTest"
     ```
     (Note: When running `run_command`, use `BypassSandbox: true` so Mockito inline mock maker works).
   - Document all changes, files touched, and verbatim test outputs in `handoff.md`.
