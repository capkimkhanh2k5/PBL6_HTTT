# Build System and Testing Infrastructure Analysis

**Investigator**: `survey_explorer_3`  
**Target Codebase**: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/backend`  
**Date**: 2026-09-10  

---

## 1. Executive Summary

The `backend` project is a **Java 21 Spring Boot** application built using **Maven** with the Maven Wrapper (`./mvnw`, version 3.9.16). The testing infrastructure relies on **JUnit 5 Jupiter** and **Mockito** with standalone and mock-based unit tests. Both compilation (`./mvnw test-compile`) and unit tests (`./mvnw test`) have been executed and verified in the environment.

The 8 required unit tests specified in `ORIGINAL_REQUEST.md` (`CreateCategoryUseCaseTest` with 5 cases, `GetCategoryTreeUseCaseTest` with 2 cases, and `DeactivateCategoryUseCaseTest` with 1 case) align seamlessly with the existing UseCase unit test conventions in the codebase (pure Mockito unit tests, zero database overhead, sub-second execution).

---

## 2. Build Tool & Runtime Environment

### 2.1 Build Tool Configuration
- **Build Tool**: Apache Maven
- **Build Configuration**: `backend/pom.xml`
- **Maven Wrapper**: `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`
- **Maven Distribution Version**: 3.9.16
- **Parent Starter**:
  ```xml
  <parent>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-parent</artifactId>
      <version>4.1.1</version>
      <relativePath/>
  </parent>
  ```
- **Java Version Configured**: Java 21 (`<java.version>21</java.version>`)
- **Compiler Plugin**:
  - `maven-compiler-plugin:3.13.0`
  - Annotation processor configured: `org.projectlombok:lombok:1.18.34`
- **Test Runner Plugin**:
  - `maven-surefire-plugin:3.5.6` with auto-detected `JUnitPlatformProvider`

### 2.2 System Runtime & Environment Prerequisites
- **JDK on System**: Eclipse Adoptium Temurin OpenJDK 21.0.10 LTS
  - Location: `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`
- **Environment Requirement**:
  The default shell environment may lack `java` on `PATH`. Any command invoking `./mvnw` must explicitly export:
  ```bash
  export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
  export PATH=$JAVA_HOME/bin:$PATH
  ```
- **Agent Sandbox Consideration**:
  Mockito 5+ uses ByteBuddy's inline mock maker, which attaches dynamically to the running JVM (`ByteBuddyAgent.installExternal()`). Under macOS Seatbelt sandbox constraints, external process attachment is restricted. Therefore, test execution commands within the agent framework require `BypassSandbox: true` (or disabling sandbox restrictions) to allow ByteBuddy self-attachment.

---

## 3. Testing Stack Analysis

### 3.1 Dependencies in `pom.xml`
The testing dependencies defined in `pom.xml` are:
1. `org.springframework.boot:spring-boot-starter-test` (test scope):
   - **JUnit 5 (Jupiter)**: `org.junit.jupiter:junit-jupiter-api`, `junit-jupiter-engine`, `junit-jupiter-params`
   - **Mockito**: `org.mockito:mockito-core`, `mockito-junit-jupiter`
   - **AssertJ**: `org.assertj:assertj-core`
   - **JSONassert**: `org.skyscreamer:jsonassert`
   - **Spring Test**: `org.springframework:spring-test`
2. Modular Spring Boot test starters:
   - `spring-boot-starter-actuator-test`
   - `spring-boot-starter-data-jpa-test`
   - `spring-boot-starter-security-test`
   - `spring-boot-starter-validation-test`
   - `spring-boot-starter-webmvc-test`
3. Integration Test Containers:
   - `org.testcontainers:testcontainers-junit-jupiter` (BOM version 2.0.5)
   - `com.redis:testcontainers-redis:2.2.4`

### 3.2 Key Testing Paradigms in Codebase
| Test Type | Example Class | Technique | Notes |
|---|---|---|---|
| **UseCase Unit Tests** | `LoginUseCaseTest`, `RegisterUseCaseTest` | Pure JUnit 5 + Mockito mocks | Fast (< 50ms), isolated, no Spring context, mocks injected in `@BeforeEach` or via `@InjectMocks` |
| **Controller Unit Tests** | `AuthenticationControllerTest` | `MockMvcBuilders.standaloneSetup(...)` | No `@SpringBootTest`, mocks UseCases, uses `MockMvc` and custom `ExceptionHandler` |
| **Domain Model Tests** | `AuthenticationTest` | Plain JUnit 5 assertions | Tests constructor, validation, accessors |
| **Adapter Tests** | `UserAccountAdapterTest` | Mockito mocks for internal APIs | Tests translation between entity/model |
| **Integration Tests** | `RateLimitFilterIntegrationTest` | `@Testcontainers`, GenericContainer Redis | Requires live Docker daemon |
| **Full Context Test** | `BackendApplicationTests` | `@SpringBootTest` | Currently `@Disabled("Fails without test database setup")` |

---

## 4. Existing Test Conventions and Structure

### 4.1 Directory Layout
Unit tests strictly mirror the production package layout under `src/test/java`:
```
src/
├── main/java/com/danasea/backend/
│   ├── config/
│   ├── modules/
│   │   ├── account/
│   │   ├── service/
│   │   │   ├── domain/models/
│   │   │   └── infrastructure/persistence/
│   │   └── systemconfig/
│   ├── security/
│   └── shared/
└── test/java/com/danasea/backend/
    ├── BackendApplicationTests.java
    └── security/authentication/
        ├── application/usecase/
        │   ├── LoginUseCaseTest.java
        │   ├── LogoutUseCaseTest.java
        │   ├── RefreshTokenUseCaseTest.java
        │   └── RegisterUseCaseTest.java
        ├── domain/model/
        ├── infrastructure/
        └── presentation/
```

### 4.2 Code Conventions Observed
1. **Naming Conventions**:
   - Class Name: `<TargetClass>Test` (e.g. `LoginUseCaseTest`, `CreateCategoryUseCaseTest`).
   - Test Method Name: `should<ExpectedOutcome>[When<Condition>]`, e.g.:
     - `shouldLoginSuccessfully()`
     - `shouldThrowWhenUserNotFound()`
     - `shouldThrowWhenPasswordMismatches()`
2. **AAA (Arrange - Act - Assert) Pattern**:
   ```java
   @Test
   void shouldRegisterSuccessfully() {
       // Arrange
       String email = "test@example.com";
       when(userAccountPort.existsByEmail(email)).thenReturn(false);
       // Act
       LoginResult result = registerUseCase.execute(email, password);
       // Assert
       assertNotNull(result);
       verify(accountInternalApi).saveRefreshToken(any(RefreshToken.class));
   }
   ```
3. **Mock Setup Convention**:
   Two patterns exist in the repository:
   - **Pattern A (Manual setup in `@BeforeEach`)**:
     ```java
     class LoginUseCaseTest {
         private UserAccountPort userAccountPort;
         private LoginUseCase loginUseCase;
         
         @BeforeEach
         void setUp() {
             userAccountPort = mock(UserAccountPort.class);
             loginUseCase = new LoginUseCase(userAccountPort, ...);
         }
     }
     ```
   - **Pattern B (MockitoExtension annotations)**:
     ```java
     @ExtendWith(MockitoExtension.class)
     class OtpEmailConsumerTest {
         @Mock
         private JavaMailSender mailSender;
         @InjectMocks
         private OtpEmailConsumer consumer;
     }
     ```
   Pattern A is preferred in UseCase tests across the codebase for explicit dependency instantiation.

---

## 5. Build and Test Verification Results

### 5.1 Verification Commands Run

#### 1. Compile Check
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin:$PATH \
./mvnw test-compile
```
**Result**: `BUILD SUCCESS` (Compiled 194 source files + 10 test files in 2.66s).

#### 2. Single UseCase Test Execution
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin:$PATH \
./mvnw test -Dtest=LoginUseCaseTest
```
**Result**: `BUILD SUCCESS` (5 tests run, 0 failures, 0 errors in 0.72s).

#### 3. All UseCase Tests Execution
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin:$PATH \
./mvnw test -Dtest="*UseCaseTest"
```
**Result**: `BUILD SUCCESS` (12 tests run, 0 failures, 0 errors in 3.28s).

#### 4. Full Suite Execution
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin:$PATH \
./mvnw test
```
**Result**: `BUILD SUCCESS` (30 tests run, 0 failures, 0 errors, 1 skipped in 6.90s).

---

## 6. Fitting the 8 Required Unit Tests into the Framework

`ORIGINAL_REQUEST.md` mandates 8 specific unit tests. Here is how each test fits directly into the testing framework.

### 6.1 Placement
- Package: `com.danasea.backend.modules.service.application.usecase` (or `com.danasea.backend.modules.category.application.usecase`)
- Directory: `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/`
- Test Classes to create:
  1. `CreateCategoryUseCaseTest.java` (5 tests)
  2. `GetCategoryTreeUseCaseTest.java` (2 tests)
  3. `DeactivateCategoryUseCaseTest.java` (1 test)

### 6.2 Test Case Specifications

#### Class 1: `CreateCategoryUseCaseTest`
Dependencies to mock:
- `CategoryRepository` (or port)
- Entity/DTO mappers if applicable

Tests:
1. **`shouldCreateRootCategorySuccessfully`**:
   - Condition: `parentId = null`, slug is unique.
   - Behavior: Creates category with `parentId == null`, `isActive == true`, saves and returns result.
2. **`shouldCreateChildCategorySuccessfully`**:
   - Condition: `parentId` is valid UUID of an existing category.
   - Behavior: Validates parent existence (`findById(parentId)` returns existing category), saves child category with `parentId`, returns result.
3. **`shouldThrowNotFoundWhenParentCategoryDoesNotExist`**:
   - Condition: `parentId` provided does not exist in repository (`findById(parentId)` returns `Optional.empty()`).
   - Behavior: Throws `CategoryNotFoundException` (or 404 domain exception), verify `save()` is never invoked.
4. **`shouldThrowWhenSlugAlreadyExists`**:
   - Condition: `existsBySlug(slug)` returns `true`.
   - Behavior: Throws `SlugAlreadyExistsException`, verify `save()` is never invoked.
5. **`shouldThrowWhenHierarchyLoopDetected`**:
   - Condition: Setting `parentId` creates an ancestor-descendant cycle (e.g. A is ancestor of B, attempting to set B as parent of A, or A -> B -> A loop).
   - Behavior: Hierarchy cycle validation triggers, throws `CategoryHierarchyLoopException` (or `InvalidCategoryHierarchyException`).

#### Class 2: `GetCategoryTreeUseCaseTest`
Dependencies to mock:
- `CategoryRepository`

Tests:
1. **`shouldReturnMultiLevelCategoryTreeSuccessfully`**:
   - Mock Data: Multi-level hierarchy (Root 1 -> Child 1.1 -> Grandchild 1.1.1; Root 2).
   - Execution: Invokes tree building logic.
   - Assertions:
     - Returns roots in top-level collection.
     - Root 1 has 1 child (`Child 1.1`).
     - `Child 1.1` has 1 child (`Grandchild 1.1.1`).
     - No infinite loop or recursive overflow.
2. **`shouldFilterInactiveCategoriesInPublicTreeButIncludeInAdminTree`**:
   - Mock Data: Categories containing active and inactive records (e.g. Category C has `isActive = false`).
   - Public Execution: `execute(false)` returns only active categories; inactive category C (and its subtree) is omitted.
   - Admin Execution: `execute(true)` returns all categories including category C.

#### Class 3: `DeactivateCategoryUseCaseTest`
Dependencies to mock:
- `CategoryRepository`
- `ServiceRepository` (or port/service checker)

Tests:
1. **`shouldThrowWhenCategoryHasActiveServices`**:
   - Condition: Category exists, but `serviceRepository.existsByCategoryIdAndStatus(...)` or active service count > 0.
   - Behavior: Throws `CategoryHasActiveServicesException`.
   - Assertion: Category `isActive` status is not modified to `false`, and repository save is not executed with deactivated status.

---

## 7. Concrete Test Implementation Blueprint

```java
package com.danasea.backend.modules.service.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateCategoryUseCaseTest {

    private CategoryRepository categoryRepository;
    private CreateCategoryUseCase createCategoryUseCase;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        createCategoryUseCase = new CreateCategoryUseCase(categoryRepository);
    }

    @Test
    void shouldCreateRootCategorySuccessfully() { ... }

    @Test
    void shouldCreateChildCategorySuccessfully() { ... }

    @Test
    void shouldThrowNotFoundWhenParentDoesNotExist() { ... }

    @Test
    void shouldThrowWhenSlugAlreadyExists() { ... }

    @Test
    void shouldThrowWhenHierarchyLoopDetected() { ... }
}
```

---

## 8. Summary of Execution Commands

| Action | Command |
|---|---|
| Set Environment | `export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home && export PATH=$JAVA_HOME/bin:$PATH` |
| Compile Project | `./mvnw compile` |
| Compile Tests | `./mvnw test-compile` |
| Run All Tests | `./mvnw test` |
| Run Categories Tests | `./mvnw test -Dtest="*Category*Test"` |
| Run Single Test Class | `./mvnw test -Dtest=CreateCategoryUseCaseTest` |
| Run Single Method | `./mvnw test -Dtest=CreateCategoryUseCaseTest#shouldCreateRootCategorySuccessfully` |
