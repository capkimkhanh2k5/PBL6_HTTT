# Handoff Report: Architecture & Data Model Survey for Vendor Profile Module

**Module**: Vendor Profile Module  
**Workspace**: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module`  
**Working Directory**: `.agents/teamwork_preview_explorer_survey_1`  
**Date**: 2026-09-10T10:47:00+07:00  

---

## 1. Observation

### 1.1 Build Setup & Environment
- **Build Tool**: Apache Maven with Maven Wrapper script (`backend/mvnw`, `backend/mvnw.cmd`).
- **Build Descriptor**: `backend/pom.xml`
  - Spring Boot parent version:
    ```xml
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.1</version>
    </parent>
    ```
  - Java Version property:
    ```xml
    <properties>
        <java.version>21</java.version>
        ...
    </properties>
    ```
  - Available JDK on system:
    - Path: `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`
    - Version: Eclipse Adoptium Temurin 21.0.10+7-LTS (OpenJDK 64-Bit Server VM).
    - Verified with: `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw -v`
      Result: `Apache Maven 3.9.16`, `Java version: 21.0.10`.
  - Compiler plugin:
    ```xml
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
        <configuration>
            <annotationProcessorPaths>
                <path>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok</artifactId>
                    <version>1.18.34</version>
                </path>
            </annotationProcessorPaths>
        </configuration>
    </plugin>
    ```
- **Dependencies Present in `pom.xml`**:
  - Web & Validation: `spring-boot-starter-webmvc`, `spring-boot-starter-validation`
  - Persistence: `spring-boot-starter-data-jpa`, `org.postgresql:postgresql` (runtime)
  - Security & JWT: `spring-boot-starter-security`, `io.jsonwebtoken:jjwt-api:0.13.0`, `jjwt-impl:0.13.0`, `jjwt-jackson:0.13.0`
  - Utilities & Infrastructure: `spring-boot-starter-actuator`, `spring-boot-starter-data-redis`, `spring-boot-starter-amqp`, `spring-boot-starter-mail`, `com.bucket4j:bucket4j-core:8.10.1`, `com.bucket4j:bucket4j-redis:8.10.1`
  - Documentation: `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1`
  - Testing: `spring-boot-starter-test`, `testcontainers-bom:2.0.5`, `testcontainers-junit-jupiter`, `testcontainers-redis:2.2.4`
- **Missing Dependency**:
  - Cloudinary SDK is **not present** in `backend/pom.xml`. The prompt mentions: `Upload documents to Cloudinary`. An abstraction port (`DocumentStoragePort` / `CloudinaryPort`) is required so that the use cases depend on an interface, and tests can mock document uploads without external Cloudinary network calls.

### 1.2 Database Migrations and Existing Tables
- **Migration Scripts**:
  - There are **no** Flyway or Liquibase migrations (`src/main/resources/db/migration` does not exist; no sql scripts found via `find_by_name`).
- **Database Schema Management**:
  - `backend/src/main/resources/application.yml` (lines 14-18):
    ```yaml
      jpa:
        hibernate:
          ddl-auto: update
        open-in-view: false
    ```
- **Existing Entities**:
  1. `UserJpaEntity` (`backend/src/main/java/com/danasea/backend/modules/account/infrastructure/persistence/entities/UserJpaEntity.java`):
     - Table: `@Table(name = "users")`
     - Inherits from `BaseJpaEntity`:
       - `id`: `UUID` (PK, generated via `@GeneratedValue(strategy = GenerationType.UUID)`)
       - `createdAt`: `OffsetDateTime` (`@CreationTimestamp`, updatable = false)
       - `updatedAt`: `OffsetDateTime` (`@UpdateTimestamp`)
     - Table columns:
       - `email`: `String`
       - `phone`: `String`
       - `passwordHash`: `String`
       - `fullName`: `String`
       - `role`: `@Enumerated(EnumType.STRING)` mapped to `com.danasea.backend.modules.account.domain.models.Role` (`CUSTOMER`, `VENDOR`, `ADMIN`)
       - `avatarUrl`: `String`
       - `isEmailVerified`: `Boolean`
       - `isLocked`: `Boolean`
       - `locale`: `String`
     - *Note on `roles`*: There is **no separate `roles` table**; roles are represented as an enum value stored directly in the `users.role` column.
  2. `VendorJpaEntity` (`backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/entities/VendorJpaEntity.java`):
     - Table: `@Table(name = "vendors")`
     - Inherits from `BaseJpaEntity` (`id`, `createdAt`, `updatedAt`).
     - Fields / columns:
       - `userId`: `UUID` (Foreign key identifier referencing `users.id`; loosely coupled, not a JPA `@OneToOne` or `@JoinColumn` relation)
       - `businessName`: `String`
       - `taxCode`: `String`
       - `address`: `String`
       - `bankAccountNumber`: `String`
       - `bankName`: `String`
       - `bankAccountHolder`: `String`
       - `verificationStatus`: `@Enumerated(EnumType.STRING)` of `VerificationStatus`
       - `verifiedBy`: `UUID`
       - `verifiedAt`: `OffsetDateTime`
       - `ratingAvg`: `BigDecimal`
       - `ratingCount`: `Integer`
       - `badgeTier`: `@Enumerated(EnumType.STRING)` of `BadgeTier`
  3. `VendorDocumentJpaEntity` (`backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/entities/VendorDocumentJpaEntity.java`):
     - Table: `@Table(name = "vendor_documents")`
     - Inherits from `BaseJpaEntity` (`id`, `createdAt`, `updatedAt`).
     - Fields / columns:
       - `vendorId`: `UUID` (Foreign key identifier referencing `vendors.id`)
       - `docType`: `@Enumerated(EnumType.STRING)` of `DocType`
       - `fileUrl`: `String`
       - `status`: `@Enumerated(EnumType.STRING)` of `DocStatus`
       - `reviewedBy`: `UUID`
       - `reviewedAt`: `OffsetDateTime`

### 1.3 Existing Repositories
- `JpaVendorRepository` (`backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/repositories/JpaVendorRepository.java`):
  ```java
  @Repository
  public interface JpaVendorRepository extends JpaRepository<VendorJpaEntity, UUID> {
  }
  ```
  *(Currently empty; needs `Optional<VendorJpaEntity> findByUserId(UUID userId)` and `boolean existsByUserId(UUID userId)`)*
- `JpaVendorDocumentRepository` (`backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/repositories/JpaVendorDocumentRepository.java`):
  ```java
  @Repository
  public interface JpaVendorDocumentRepository extends JpaRepository<VendorDocumentJpaEntity, UUID> {
  }
  ```
  *(Currently empty; needs `List<VendorDocumentJpaEntity> findAllByVendorId(UUID vendorId)`)*
- `JpaUserRepository` (`backend/src/main/java/com/danasea/backend/modules/account/infrastructure/persistence/repositories/JpaUserRepository.java`):
  ```java
  public interface JpaUserRepository extends JpaRepository<UserJpaEntity, UUID> {
      java.util.Optional<UserJpaEntity> findByEmail(String email);
      boolean existsByEmail(String email);
  }
  ```

### 1.4 Existing Enums and Models in `modules/vendor`
- `com.danasea.backend.modules.vendor.domain.models.VerificationStatus`:
  ```java
  public enum VerificationStatus {
      PENDING, APPROVED, REJECTED
  }
  ```
- `com.danasea.backend.modules.vendor.domain.models.BadgeTier`:
  ```java
  public enum BadgeTier {
      NONE, VERIFIED, TOP_RATED
  }
  ```
- `com.danasea.backend.modules.vendor.domain.models.DocType`:
  ```java
  public enum DocType {
      BUSINESS_LICENSE, SAFETY_CERT
  }
  ```
- `com.danasea.backend.modules.vendor.domain.models.DocStatus`:
  ```java
  public enum DocStatus {
      PENDING, APPROVED, REJECTED
  }
  ```
- `com.danasea.backend.modules.account.domain.models.Role`:
  ```java
  public enum Role {
      CUSTOMER, VENDOR, ADMIN
  }
  ```

### 1.5 Architecture Layers & Inter-Module Conventions
- **Architectural Style**: Modular Monolith following Clean/Hexagonal principles.
  - `modules/account`: Manages users and authentication data. Exposes `AccountInternalApi`:
    ```java
    public interface AccountInternalApi {
        Optional<User> findUserByEmail(String email);
        Optional<User> findUserById(UUID id);
        boolean existsByEmail(String email);
        User saveUser(User user);
        ...
    }
    ```
  - Standard module directory layout:
    - `domain/models/`: Rich/Anemic domain entities & enums.
    - `domain/exception/`: Business exceptions extending `RuntimeException`.
    - `application/usecase/`: Single-responsibility use cases (`RegisterVendorProfileUseCase`, `UpdateVendorProfileUseCase`, `UploadVendorDocumentUseCase`, etc.).
    - `application/port/`: Interface definitions for external secondary adapters (e.g. `DocumentStoragePort`).
    - `application/result/`: Return records / DTOs from use cases.
    - `infrastructure/persistence/entities/`: JPA `@Entity` classes extending `BaseJpaEntity`.
    - `infrastructure/persistence/repositories/`: Spring Data JPA repositories extending `JpaRepository`.
    - `infrastructure/mapper/`: `@Component` mapping domain models to/from JPA entities.
    - `presentation/`: `@RestController`, Request/Response DTO records (`@Valid`), and `@RestControllerAdvice` exception handler mapping exceptions to `com.danasea.backend.shared.presentation.ErrorResponse(code, message)`.

### 1.6 Security Context & Authorization Mechanics
- `SecurityConfig.java` (`backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`):
  - `@EnableMethodSecurity` is enabled.
  - Public endpoints: `/api/auth/**`, `/actuator/health`, Swagger.
  - All other routes: `.anyRequest().authenticated()`.
  - `accessDeniedHandler` returns HTTP 403: `{"code":"ACCESS_DENIED","message":"Access denied"}`.
- `JwtAuthenticationFilter.java`:
  - Parses Bearer token.
  - Creates `UsernamePasswordAuthenticationToken(subject.email(), null, authorities)`.
  - Roles are prefixed with `ROLE_` (e.g. `ROLE_CUSTOMER`, `ROLE_VENDOR`, `ROLE_ADMIN`).
  - Thus, endpoints can be secured with `@PreAuthorize("hasRole('VENDOR')")` or `@PreAuthorize("hasRole('CUSTOMER')")`.
  - Controller methods can receive `Principal principal` (or `@AuthenticationPrincipal String email`) to obtain the logged-in user's email.
  - User ID is retrieved via `accountInternalApi.findUserByEmail(principal.getName()).map(User::getId)`.

---

## 2. Logic Chain

1. **Build & Tooling Compatibility (Obs. 1.1)**:
   - The project uses Maven 3.9.x with Java 21. Running `./mvnw test-compile` with `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` succeeds cleanly across all 194 main source files and 10 test files.
   - Cloudinary is not present as an external Maven dependency. To satisfy requirement R1 ("Upload documents to Cloudinary") cleanly without network coupling in tests, a `DocumentStoragePort` interface must be introduced in the application layer, implemented by a `CloudinaryStorageAdapter` (or stub) in infrastructure.

2. **Schema & Entity Design (Obs. 1.2, 1.3, 1.4)**:
   - Database schemas are managed automatically by Hibernate (`ddl-auto: update`).
   - `UserJpaEntity`, `VendorJpaEntity`, and `VendorDocumentJpaEntity` are already defined and align with the database columns specified in the requirements.
   - Entity relationships between modules follow a decoupled identifier pattern (`userId: UUID` in `VendorJpaEntity`, `vendorId: UUID` in `VendorDocumentJpaEntity`) rather than `@OneToOne` or `@ManyToOne` entity joins across module boundaries. This preserves modular monolithic isolation.
   - `JpaVendorRepository` and `JpaVendorDocumentRepository` currently lack finder queries and need methods added (`findByUserId`, `existsByUserId`, `findAllByVendorId`).

3. **Vendor Registration Flow (R1, R2)**:
   - When a CUSTOMER invokes `POST /api/vendor/profile`:
     1. The user identity is extracted from `SecurityContext` (`principal.getName()` -> `accountInternalApi.findUserByEmail(email)`).
     2. If user `isLocked == true`, throw `UserLockedException` (handled as 403/400).
     3. Check if vendor record already exists for `userId`: `vendorRepository.existsByUserId(userId)`. If true, throw `VendorAlreadyExistsException` (handled as 409 Conflict).
     4. Create and persist `Vendor` with `verificationStatus = PENDING`, `ratingAvg = BigDecimal.ZERO`, `ratingCount = 0`, `badgeTier = BadgeTier.NONE`.
     5. Update user role: change `user.role` from `CUSTOMER` to `VENDOR` and call `accountInternalApi.saveUser(user)`.

4. **Vendor Profile Update Flow & Mass Assignment Protection (R1, R2, AC)**:
   - Requirement AC states: "The PATCH endpoint uses a specific DTO that strictly excludes verification_status, rating_avg, and badge_tier."
   - Requirement AC states: "The vendor identity in the PATCH endpoint is derived directly from the SecurityContext (user ID), not from user-provided request parameters or path variables."
   - Create `UpdateVendorProfileRequest(String businessName, String taxCode, String address, String bankAccountNumber, String bankName, String bankAccountHolder)`. This DTO physically omits `verificationStatus`, `ratingAvg`, `ratingCount`, `badgeTier`, `verifiedBy`, and `userId`.
   - In `UpdateVendorProfileUseCase`: load vendor by `userId` resolved from `SecurityContext`. If not found, throw `VendorNotFoundException` (404). Update only the allowed non-null fields from the DTO.

5. **Document Upload Flow (R1, R2)**:
   - In `POST /api/vendor/documents`:
     1. Retrieve vendor by `userId` from `SecurityContext`. If vendor does not exist, throw `VendorNotFoundException` (404/409 as tested in `UploadVendorDocumentUseCaseTest`).
     2. Validate `doc_type`: must be a valid `DocType` enum (`BUSINESS_LICENSE` or `SAFETY_CERT`). If invalid, throw `InvalidDocTypeException` (400).
     3. Upload file via `DocumentStoragePort.upload(file)` to get `fileUrl`.
     4. Save `VendorDocumentJpaEntity` with:
        - `vendorId = vendor.getId()`
        - `docType = docType`
        - `fileUrl = fileUrl`
        - `status = DocStatus.PENDING`
        - `reviewedBy = null`
        - `reviewedAt = null`
   - In `GET /api/vendor/documents`:
     - Load all documents for `vendorId`: `vendorDocumentRepository.findAllByVendorId(vendor.getId())` and map to `List<VendorDocumentResponse>`.

6. **Controller Security & Route Authorization (R3)**:
   - `GET /api/vendor/profile`:
     - Accessible by authenticated VENDOR or CUSTOMER (if registered).
     - If a CUSTOMER without a vendor profile requests `GET /api/vendor/profile`, return 404 (`VendorNotFoundException`).
   - If an ADMIN attempts to access VENDOR routes (e.g. `POST /api/vendor/profile` or document endpoints restricted to VENDOR), `@PreAuthorize("hasRole('VENDOR')")` or custom role enforcement returns 403 Forbidden.

---

## 3. Caveats

1. **Test Environment / Mockito in macOS Sandbox**:
   - In macOS sandbox environment, running Mockito with Java 21's default `mock-maker-inline` attempts dynamic agent self-attachment via process socket IPC (`jdk.attach/sun.tools.attach.VirtualMachineImpl.connect`), which fails with `IOException: Operation not permitted`.
   - **Remediation**:
     - Tests should use Mockito's `mock-maker-subclass` (created in `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`), OR the surefire plugin should configure the ByteBuddy javaagent, OR tests should mock interfaces. All domain ports and internal APIs in this architecture are interfaces, which subclass mock maker handles cleanly.
2. **Database Container Tests**:
   - `BackendApplicationTests` is `@Disabled("Fails without test database setup")` and `RateLimitFilterIntegrationTest` failed due to no local Docker daemon running in the sandbox.
   - Use case tests and MockMvc controller tests should be pure unit / standalone MockMvc tests (e.g. `MockMvcBuilders.standaloneSetup(...)` as shown in `AuthenticationControllerTest.java`) that do NOT require spinning up Docker or PostgreSQL containers.
3. **Cloudinary SDK**:
   - Because no Cloudinary client is currently declared in `pom.xml`, creating a `DocumentStoragePort` interface and a mockable adapter avoids build breakage and keeps unit tests 100% deterministic.

---

## 4. Conclusion

The codebase is well-structured as a clean Modular Monolith. The necessary entities (`VendorJpaEntity`, `VendorDocumentJpaEntity`, `UserJpaEntity`) and enums (`VerificationStatus`, `BadgeTier`, `DocType`, `DocStatus`, `Role`) are already in place in `modules/vendor` and `modules/account`.

### Implementation Plan for Implementer:
1. **Extend Repositories**:
   - `JpaVendorRepository`: Add `Optional<VendorJpaEntity> findByUserId(UUID userId)` and `boolean existsByUserId(UUID userId)`.
   - `JpaVendorDocumentRepository`: Add `List<VendorDocumentJpaEntity> findAllByVendorId(UUID vendorId)`.
2. **Create Mappers**:
   - `modules/vendor/infrastructure/mapper/VendorMapper.java`
   - `modules/vendor/infrastructure/mapper/VendorDocumentMapper.java`
3. **Create Storage Port & Adapter**:
   - `modules/vendor/application/port/DocumentStoragePort.java`
   - `modules/vendor/infrastructure/storage/MockCloudinaryStorageAdapter.java`
4. **Create Domain Exceptions**:
   - `VendorAlreadyExistsException` (409)
   - `VendorNotFoundException` (404)
   - `UserLockedException` (403 or 400)
   - `InvalidDocTypeException` (400)
5. **Implement Application Use Cases**:
   - `RegisterVendorProfileUseCase`: Validates user not locked, vendor doesn't exist, persists vendor (`PENDING`), updates user role to `VENDOR` via `AccountInternalApi.saveUser(user)`.
   - `GetVendorProfileUseCase`: Resolves user by email/ID, loads vendor, returns `VendorProfileResponse`.
   - `UpdateVendorProfileUseCase`: Resolves vendor by `userId` from security context, applies only non-null allowed profile fields from `UpdateVendorProfileRequest`.
   - `UploadVendorDocumentUseCase`: Verifies vendor exists, validates `docType`, uploads file via storage port, persists document with `PENDING` and null review fields.
   - `GetVendorDocumentsUseCase`: Returns documents for the vendor.
6. **Implement Presentation Layer**:
   - DTOs: `RegisterVendorProfileRequest`, `UpdateVendorProfileRequest` (strict, excluding verification status/ratings), `VendorProfileResponse`, `VendorDocumentResponse`.
   - `VendorProfileController` (`/api/vendor/profile` and `/api/vendor/documents`).
   - `VendorExceptionHandler` (`@RestControllerAdvice`).
7. **Implement Test Suite**:
   - `RegisterVendorProfileUseCaseTest`
   - `UpdateVendorProfileUseCaseTest`
   - `UploadVendorDocumentUseCaseTest`
   - `VendorProfileControllerTest` (standalone MockMvc)
   - Add `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` containing `mock-maker-subclass` to guarantee seamless execution on Java 21 in macOS sandbox.

---

## 5. Verification Method

To independently verify all findings:
1. **Verify JDK and Maven Build**:
   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test-compile
   ```
   *Expected*: BUILD SUCCESS (compiles 194 source files with javac release 21).
2. **Inspect Entity Files**:
   - Check `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/entities/VendorJpaEntity.java`
   - Check `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/entities/VendorDocumentJpaEntity.java`
   - Check `backend/src/main/java/com/danasea/backend/modules/account/infrastructure/persistence/entities/UserJpaEntity.java`
3. **Inspect Enums**:
   - Check `backend/src/main/java/com/danasea/backend/modules/vendor/domain/models/VerificationStatus.java`
   - Check `backend/src/main/java/com/danasea/backend/modules/vendor/domain/models/BadgeTier.java`
   - Check `backend/src/main/java/com/danasea/backend/modules/vendor/domain/models/DocType.java`
   - Check `backend/src/main/java/com/danasea/backend/modules/vendor/domain/models/DocStatus.java`
4. **Inspect Inter-Module API**:
   - Check `backend/src/main/java/com/danasea/backend/modules/account/application/api/AccountInternalApi.java`
