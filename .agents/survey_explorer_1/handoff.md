# Handoff Report: Spring Boot Backend Architecture & Categories Module Survey

**Agent**: `survey_explorer_1`  
**Date**: 2026-09-10  
**Target Module**: Categories Module API & Unit Tests  
**Scope**: Codebase in `backend/`

---

## 1. Observation

1. **Clean Architecture Rules Document**:
   - File path: `backend/docs/Clean_Architecture_Rules.md` (Lines 1–235).
   - Verbatim quote (lines 12–16):
     ```text
     Presentation → Application → Domain
                        ↑
                   Infrastructure
     ```
   - Verbatim quote (lines 160–177):
     ```text
     modules/
     ├── module-a/
     │   ├── domain/
     │   ├── application/
     │   ├── infrastructure/
     │   └── presentation/
     ```
2. **Build and Dependencies**:
   - File path: `backend/pom.xml` (Lines 1–213).
   - Spring Boot version: `4.1.1` (parent POM), Java version: `21`.
   - Core libraries present: `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-webmvc`, `postgresql`, `lombok`.
   - Libraries absent: No `mapstruct` or `modelmapper`. Only `lombok` configured in `maven-compiler-plugin` annotationProcessorPaths (lines 200–207).
3. **Existing Category and Service Entities**:
   - File path: `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Category.java`:
     - Extends `BaseDomainModel` (`id`, `createdAt`, `updatedAt`).
     - Contains fields: `name`, `nameEn`, `slug`, `parentId`, `iconUrl`, `isActive`.
   - File path: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/CategoryJpaEntity.java`:
     - Line 13: `@Table(name = "categorys")` (typo vs "categories" in docx).
     - Extends `BaseJpaEntity` (`id` UUID, `createdAt`, `updatedAt`).
     - Contains fields: `name`, `nameEn`, `slug`, `parentId`, `iconUrl`, `isActive`.
   - File path: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaCategoryRepository.java`:
     - Extends `JpaRepository<CategoryJpaEntity, UUID>`.
   - File path: `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Service.java`:
     - Line 14: `private UUID categoryId;`
     - Line 27: `private ServiceStatus status;`
   - File path: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceJpaEntity.java`:
     - Line 20: `private UUID categoryId;`
     - Line 47: `@Enumerated(EnumType.STRING) private ServiceStatus status;`
   - File path: `backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceStatus.java`:
     - Line 4: `DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, PAUSED`
   - File path: `backend/docs/DANASEA_Database_Design.docx`:
     - P182-P205: `categories` table with columns `id (UUID PK), name (VARCHAR 100), name_en (VARCHAR 100), slug (VARCHAR 120 UNIQUE), parent_id (UUID FK -> categories.id), icon_url (TEXT), is_active (BOOLEAN DEFAULT true)`.
     - P240-P243: `services.status` is `ENUM(DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, PAUSED)`. Active services on the marketplace are `PUBLISHED`.
4. **Exception Handling Conventions**:
   - File path: `backend/src/main/java/com/danasea/backend/shared/presentation/ErrorResponse.java`:
     - Record: `public record ErrorResponse(String code, String message)`.
   - File path: `backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationExceptionHandler.java`:
     - Uses `@RestControllerAdvice`.
     - Catches domain exceptions and maps them to `ResponseEntity<ErrorResponse>`.
     - Handles `MethodArgumentNotValidException` mapping field validation errors to `ErrorResponse("INVALID_INPUT", ...)`.
   - Domain exceptions (e.g. `EmailAlreadyUsedException.java`): extend `RuntimeException`.
5. **DTO and Mapper Conventions**:
   - File path: `backend/src/main/java/com/danasea/backend/security/authentication/presentation/dto/LoginRequest.java`:
     - Written as Java `record`.
   - File path: `backend/src/main/java/com/danasea/backend/modules/account/infrastructure/mapper/UserMapper.java`:
     - Annotated with Spring `@Component`.
     - Pure manual mappings (`toDomain`, `toEntity`).
6. **Existing Reference Implementation for UseCases**:
   - File path: `backend/src/main/java/com/danasea/backend/security/authentication/application/usecase/LoginUseCase.java`:
     - Pure Java class with `@RequiredArgsConstructor`.
     - Injected with interfaces from `application.port`.
     - Declared as `@Bean` in `backend/src/main/java/com/danasea/backend/config/ApplicationBeans.java` (lines 25–38).
   - File path: `backend/src/test/java/com/danasea/backend/security/authentication/application/usecase/LoginUseCaseTest.java`:
     - Pure JUnit 5 + Mockito unit tests without loading Spring context.
7. **Security Configuration**:
   - File path: `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`:
     - Line 27: `@EnableMethodSecurity` is enabled.
     - Lines 54–65: Public permitAll only covers auth, health, swagger. `GET /api/categories` must be permitted for public access. Admin endpoints require `ROLE_ADMIN`.

---

## 2. Logic Chain

1. **From Observation 1 & 6 to Architecture Pattern**:
   - The project architecture is explicitly defined in `Clean_Architecture_Rules.md` as Modular Clean Architecture.
   - Reference implementation in `security/authentication` and `account` proves that UseCases (`application/usecase`) are pure POJOs depending on output ports (`application/port`), with persistence adapters (`infrastructure/persistence`) implementing ports, and controllers (`presentation`) calling use cases.
   - Therefore, the Categories module must be implemented using this exact Clean Architecture / UseCase pattern, rather than a generic 3-tier Controller -> Service -> Repository.
2. **From Observation 3 to Entity Design and Relationships**:
   - `Category.java`, `CategoryJpaEntity.java`, and `JpaCategoryRepository.java` already exist under `com.danasea.backend.modules.service`.
   - In all entities across the codebase (e.g. `ServiceJpaEntity`, `ServiceImageJpaEntity`, `WishlistJpaEntity`), foreign relations are stored directly as primitive/wrapper `UUID` fields, without `@ManyToOne` or `@OneToMany` annotations.
   - Therefore, `CategoryJpaEntity` and `ServiceJpaEntity` must maintain direct `UUID parentId` and `UUID categoryId` references.
   - Furthermore, `CategoryJpaEntity` has `@Table(name = "categorys")` which should be corrected to `@Table(name = "categories")` matching the official schema design.
3. **From Observation 3 to Active Service Deactivation Rule**:
   - `ServiceJpaEntity` has no `is_active` boolean column, but contains `ServiceStatus status`.
   - Per database specification, `ServiceStatus.PUBLISHED` denotes active services live on the market.
   - Therefore, checking if a category has active services before deactivation (`CategoryHasActiveServicesException`) must query `JpaServiceRepository` for any service referencing `categoryId` with `status == ServiceStatus.PUBLISHED`.
4. **From Observation 4 to Exception Handling Design**:
   - The codebase standardizes error responses on `ErrorResponse(String code, String message)`.
   - Domain exceptions (`CategoryNotFoundException`, `SlugAlreadyExistsException`, `CategoryHasActiveServicesException`, `CategoryHierarchyLoopException`) must extend `RuntimeException`.
   - A `@RestControllerAdvice` (e.g. `CategoryExceptionHandler`) will catch these exceptions and return `HttpStatus.NOT_FOUND` (404), `HttpStatus.CONFLICT` (409), and `HttpStatus.BAD_REQUEST` (400) with matching error codes.
5. **From Observation 2 & 5 to DTO and Mapper Implementation**:
   - Without MapStruct, DTO mappings must be implemented via manual Spring `@Component` mapper (`CategoryMapper`) in `infrastructure.mapper`.
   - Requests and responses must be Java `record`s in `presentation.dto`.
6. **From Observation 6 & Acceptance Criteria to Test Implementation**:
   - Acceptance criteria require unit tests for `CreateCategoryUseCaseTest`, `GetCategoryTreeUseCaseTest`, and `DeactivateCategoryUseCaseTest`.
   - Following `LoginUseCaseTest.java`, tests should be fast, isolated unit tests using JUnit 5 and Mockito, mocking the `CategoryRepositoryPort` and `ServiceQueryPort`.

---

## 3. Caveats

1. **Package placement choice**:
   - `Category` entities currently reside in `com.danasea.backend.modules.service`. Implementing the use cases inside `com.danasea.backend.modules.service` is the cleanest and least disruptive path. If the orchestrator/implementer decides to create a standalone module `com.danasea.backend.modules.category`, the existing files in `modules/service` must be moved cleanly.
2. **Local Java Runtime in Sandbox**:
   - Terminal execution of `./mvnw` in the current sandbox failed with "Unable to locate a Java Runtime" because macOS `/usr/libexec/java_home` cannot locate the JDK without specific environment variables or outside-sandbox access. Implementers should verify unit tests in an environment with active JDK 21.
3. **Active Service Definition**:
   - We observed `ServiceStatus` has values: `DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, PAUSED`. We concluded `PUBLISHED` represents active services. If `PAUSED` services should also prevent deactivation, the query can check `status IN (PUBLISHED, PAUSED)`.

---

## 4. Conclusion

The Spring Boot backend is organized under a well-structured **Modular Clean Architecture**:
1. **Module Placement**: Implement the Categories feature within `com.danasea.backend.modules.service` (or as a cohesive `category` sub-package under it) utilizing the pre-existing `Category` domain model, `CategoryJpaEntity`, and `JpaCategoryRepository`.
2. **Layers**:
   - Domain: `Category`, domain exceptions (`SlugAlreadyExistsException`, `CategoryNotFoundException`, `CategoryHasActiveServicesException`, `CategoryHierarchyLoopException`).
   - Application: `CategoryRepositoryPort`, UseCases (`CreateCategoryUseCase`, `GetCategoryTreeUseCase`, `DeactivateCategoryUseCase`, `UpdateCategoryUseCase`), Result records.
   - Infrastructure: `CategoryPersistenceAdapter` implementing port, `CategoryMapper` (`@Component`), updated `JpaCategoryRepository` and `JpaServiceRepository`.
   - Presentation: `CategoryController` (or `AdminCategoryController` & `PublicCategoryController`), `CategoryExceptionHandler` (`@RestControllerAdvice`), DTO records (`CreateCategoryRequest`, `CategoryResponse`, `CategoryTreeResponse`).
3. **Database Fix**: Change `@Table(name = "categorys")` in `CategoryJpaEntity.java` to `@Table(name = "categories")`.
4. **Security**: Configure `GET /api/categories` in `SecurityConfig.java` under `permitAll()`, and secure `/api/admin/categories/**` with `@PreAuthorize("hasRole('ADMIN')")`.
5. **Unit Tests**: Implement unit tests for `CreateCategoryUseCaseTest`, `GetCategoryTreeUseCaseTest`, and `DeactivateCategoryUseCaseTest` with JUnit 5 + Mockito according to the project's testing patterns.

Full details are documented in `.agents/survey_explorer_1/analysis.md`.

---

## 5. Verification Method

To independently verify these findings:
1. **Inspect Clean Architecture Specification**:
   - View `backend/docs/Clean_Architecture_Rules.md` to confirm the layer definitions and dependency rules.
2. **Inspect Reference Implementations**:
   - View `backend/src/main/java/com/danasea/backend/security/authentication/application/usecase/LoginUseCase.java` and `LoginUseCaseTest.java`.
   - View `backend/src/main/java/com/danasea/backend/modules/account/infrastructure/mapper/UserMapper.java`.
   - View `backend/src/main/java/com/danasea/backend/shared/presentation/ErrorResponse.java`.
3. **Inspect Existing Entities**:
   - View `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Category.java`.
   - View `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/CategoryJpaEntity.java`.
   - View `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceJpaEntity.java`.
   - View `backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceStatus.java`.
4. **Invalidation Conditions**:
   - If the codebase transitions to traditional 3-tier Controller -> Service -> Repository without UseCases, or adopts an ORM mapping model with `@ManyToOne` / `@OneToMany`, the conclusions here would need revision.
