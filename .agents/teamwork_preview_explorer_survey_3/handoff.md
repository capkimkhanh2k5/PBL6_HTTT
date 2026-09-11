# Handoff Report: Testing & Edge Cases Survey

**Agent**: Explorer 3 (Testing & Edge Cases)  
**Date**: 2026-09-10  
**Target Module**: Public Catalog, Wishlist, Recently Viewed (`modules/service`)

---

## 1. Observation

### 1.1 Build Tool, JDK & Test Dependencies
- **Build Tool**: Maven Wrapper (`./mvnw`), Maven version `3.9.16`.
- **JDK**: Java 21 (`Eclipse Adoptium Temurin-21.0.10+7-LTS` located at `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`).
- **Dependencies (`backend/pom.xml`)**:
  - Spring Boot Starter Parent: version `4.1.1` (`backend/pom.xml:8`).
  - Test dependencies (`backend/pom.xml:91-119, 175-186`):
    - `spring-boot-starter-actuator-test` (scope `test`)
    - `spring-boot-starter-data-jpa-test` (scope `test`)
    - `spring-boot-starter-security-test` (scope `test`)
    - `spring-boot-starter-validation-test` (scope `test`)
    - `spring-boot-starter-webmvc-test` (scope `test`)
    - `spring-boot-starter-test` (scope `test`, includes JUnit 5 Jupiter, Mockito 5.23.0, AssertJ, Hamcrest)
    - `testcontainers-junit-jupiter` (version 2.0.5 via BOM)
    - `testcontainers-redis` (version 2.2.4)
  - Database: `org.postgresql:postgresql` runtime dependency only (`backend/pom.xml:75-78`). H2 is **not** present in `pom.xml`.

### 1.2 Test Execution Findings & Failures Out-of-the-Box
When running `./mvnw test` in `backend`, the build failed with 2 distinct root causes:
1. **Mockito ByteBuddy Self-Attachment Failure on Java 21**:
   - Error log verbatim (`backend/target/surefire-reports/com.danasea.backend.security.authentication.application.usecase.LoginUseCaseTest.txt:32-56`):
     ```text
     Caused by: org.mockito.exceptions.base.MockitoInitializationException: 
     Could not initialize inline Byte Buddy mock maker.
     It appears as if your JDK does not supply a working agent attachment mechanism.
     Java               : 21
     JVM vendor name    : Eclipse Adoptium
     JVM vendor version : 21.0.10+7-LTS
     OS name            : Mac OS X
     OS version         : 26.4
     Caused by: java.lang.IllegalStateException: Could not self-attach to current VM using external process - set a property net.bytebuddy.agent.attacher.dump to dump the process output to a file at the specified location
     at net.bytebuddy.agent.ByteBuddyAgent.installExternal(ByteBuddyAgent.java:710)
     ```
   - **Verification of fix**: Specifying `-javaagent:/Users/capkimkhanh/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.11/byte-buddy-agent-1.18.11.jar` completely resolves the error. All Mockito-based unit and MockMvc tests pass synchronously.
2. **Docker Dependency in Integration Test**:
   - `RateLimitFilterIntegrationTest` (`backend/src/test/.../RateLimitFilterIntegrationTest.java:24-35`) uses `@Testcontainers` with `redis:7.0-alpine`. If Docker daemon is inactive or inaccessible in the execution environment, it throws:
     ```text
     java.lang.IllegalStateException: Could not find a valid Docker environment.
     ```
   - When excluding `RateLimitFilterIntegrationTest` (e.g. `-Dtest="*Test,!RateLimitFilterIntegrationTest"`), all 27 unit tests pass in 4.3 seconds with 0 failures and 0 errors.

### 1.3 Existing Test Patterns & Conventions
- **Unit UseCase Tests** (e.g. `LoginUseCaseTest.java:22-47`):
  - Pure JUnit 5 + Mockito mocks (`mock(...)` or `@Mock` + `@InjectMocks`).
  - No Spring Context loading (runs in <0.3s).
- **Controller Tests** (e.g. `AuthenticationControllerTest.java:34-64`):
  - Uses `@ExtendWith(MockitoExtension.class)`.
  - Builds isolated MockMvc using `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(...).build()`.
  - Validates HTTP statuses, JSON paths (`jsonPath("$.code")`), and cookies without spinning up the entire Spring Boot server or database.
- **Root Application Test** (`BackendApplicationTests.java:7-8`):
  - Annotated with `@Disabled("Fails without test database setup")` because full context loading requires a live PostgreSQL instance.

### 1.4 Current State of `modules/service`
- Domain models present: `Service.java`, `ServiceStatus.java` (`DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `REJECTED`, `PAUSED`), `RecentlyViewed.java`, `Wishlist.java`, `Category.java`.
- JPA entities present: `ServiceJpaEntity.java`, `RecentlyViewedJpaEntity.java`, `WishlistJpaEntity.java`, etc.
- JPA repositories present: `JpaServiceRepository.java`, `JpaRecentlyViewedRepository.java`, `JpaWishlistRepository.java` (basic `JpaRepository<T, UUID>` interfaces with no custom queries yet).
- **Missing**: No application use cases, no domain exception classes, no presentation controllers, and no test classes currently exist in `modules/service`.

### 1.5 Security Configuration
- `SecurityConfig.java:53-65`:
  - `GET /api/services` and `GET /api/services/{id}` are currently **not** in `permitAll()`.
  - Currently, all routes except `/api/auth/**`, `/actuator/health`, and Swagger require authentication (`.anyRequest().authenticated()`).

---

## 2. Logic Chain

1. **Test Execution & JDK Compatibility**:
   - *Premise*: Java 21 restricts dynamic agent attachment, and macOS environment sandboxing prevents ByteBuddy from spawning an external process to self-attach to the target JVM process.
   - *Observation*: Mockito 5 inline mock maker fails at `ByteBuddyAgent.installExternal` when executed with standard `./mvnw test`.
   - *Inference*: Test runner configuration must either:
     - Load `byte-buddy-agent.jar` at JVM startup via maven-surefire `<argLine>-javaagent:${settings.localRepository}/.../byte-buddy-agent-...jar</argLine>`, OR
     - Configure Mockito to use `mock-maker-subclass` via `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`.
   - *Conclusion*: Unit and controller tests for the Public Catalog module can run without Docker or DB if built using pure Mockito and `MockMvcBuilders.standaloneSetup`, with the Java agent configured.

2. **R4 Automated Test Requirements**:
   - `SearchServicesUseCaseTest`:
     - Must test search filtering by: keyword, category, location (radius from lat/lng), price range (`minPrice` / `maxPrice`), and status constraint.
     - Critical assertion: Services with status `DRAFT`, `PENDING_REVIEW`, `REJECTED`, or `PAUSED` must **never** be returned.
   - `GetServiceDetailUseCaseTest`:
     - Must test fetching detail for `PUBLISHED` service.
     - Must test 404 response / `ServiceNotFoundException` when service does not exist OR when service status is `DRAFT`, `PENDING_REVIEW`, `REJECTED`, or `PAUSED`.
     - Must test that `view_count` is incremented.
     - Must test recording recently viewed for both authenticated users (`userId`) and guests (`sessionId`).
   - `RecordRecentlyViewedUseCaseTest`:
     - Must test upsert behavior: If user views Service A twice, `viewedAt` is updated; no duplicate record is created.
     - Must test guest session tracking (`sessionId`).
     - Must validate input (rejecting when both `userId` and `sessionId` are null).
   - `WishlistUseCaseTest`:
     - Must test adding service to wishlist.
     - Must test **idempotent** add: adding an already wishlisted service must succeed without duplicate entry or 500 error.
     - Must test graceful removal: deleting a service that is not in wishlist must succeed gracefully (idempotent delete).
     - Must reject adding non-PUBLISHED or non-existent services.
   - `CatalogControllerTest`:
     - Must verify endpoint paths, query parameter parsing, HTTP status codes (200, 404, 400, 401).
     - Must verify access control: `GET /api/services` and `GET /api/services/{id}` are publicly accessible without authentication.
     - Must verify wishlist endpoints require authenticated user (`401 Unauthorized` when unauthenticated).

3. **Edge Case Analysis & Technical Mitigations**:
   - **Edge Case 1: Race Conditions on `view_count`**:
     - *Risk*: Concurrent reads and writes lead to lost view count updates.
     - *Solution*: Do not read-modify-write in Java memory. Use atomic SQL update:
       ```sql
       UPDATE services SET view_count = COALESCE(view_count, 0) + 1 WHERE id = :id AND status = 'PUBLISHED'
       ```
       In `JpaServiceRepository`, use `@Modifying @Query`.
   - **Edge Case 2: Idempotent Wishlist Adds**:
     - *Risk*: Double-clicks or rapid calls cause duplicate rows or unhandled unique constraint violations (`DataIntegrityViolationException`).
     - *Solution*: Add unique constraint on `(user_id, service_id)` in `WishlistJpaEntity` / DB. In `WishlistUseCase`, check existence before insert or handle duplicate gracefully.
   - **Edge Case 3: 404 for Draft/Non-Published Services**:
     - *Risk*: Exposing private or unapproved vendor drafts.
     - *Solution*: `GetServiceDetailUseCase` must filter by `status == PUBLISHED` and throw `ServiceNotFoundException` (mapped to HTTP 404 in `@RestControllerAdvice`).
   - **Edge Case 4: Guest vs User Recently Viewed Upserting**:
     - *Risk*: Duplicate rows accumulating in `recently_vieweds` for the same service and user/session.
     - *Solution*: Repository query `findFirstByUserIdAndServiceId` or `findFirstBySessionIdAndServiceId`. If present, update `viewedAt = OffsetDateTime.now()`; if absent, create new record.

---

## 3. Caveats

1. **Docker Environment**: `RateLimitFilterIntegrationTest` depends on a live Docker container running Redis. In local environments where Docker is not running or restricted, running all tests via `./mvnw test` without excluding `RateLimitFilterIntegrationTest` will report a build failure due to that single test.
2. **Spring Context Integration Tests**: Full `@SpringBootTest` tests currently require a running PostgreSQL instance because H2 is not included in `pom.xml`. If full integration tests with database persistence are desired in the future, Testcontainers PostgreSQL or H2 test dependency must be configured.
3. **Session Identification Header/Cookie**: The specification requires guest tracking via `session_id`. The exact HTTP transport (header `X-Session-Id` vs cookie `session_id`) should be standardized in the controller (recommended: check header `X-Session-Id` first, fallback to cookie `session_id`).

---

## 4. Conclusion

- The test suite architecture should follow the existing Clean Architecture patterns:
  1. Fast, isolated unit tests for Use Cases using JUnit 5 + Mockito (`SearchServicesUseCaseTest`, `GetServiceDetailUseCaseTest`, `RecordRecentlyViewedUseCaseTest`, `WishlistUseCaseTest`).
  2. MockMvc unit tests for Controllers using `MockMvcBuilders.standaloneSetup` (`CatalogControllerTest`).
- Test execution command for the project:
  ```bash
  export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
  export PATH=$JAVA_HOME/bin:$PATH
  ./mvnw test -Dtest="*Test,!RateLimitFilterIntegrationTest" -DargLine="-javaagent:${HOME}/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.11/byte-buddy-agent-1.18.11.jar"
  ```
- All four required edge cases have clear, proven architectural solutions ready to be integrated into the use cases, repositories, and entities.

---

## 5. Verification Method

To independently verify all findings:
1. **Inspect pom.xml & dependencies**:
   `cat backend/pom.xml`
2. **Verify Java 21 path**:
   `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java -version`
3. **Reproduce the test execution behavior**:
   - Run unit test suite:
     ```bash
     export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
     cd /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/backend
     ./mvnw test -Dtest="LoginUseCaseTest,AuthenticationControllerTest" -DargLine="-javaagent:${HOME}/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.11/byte-buddy-agent-1.18.11.jar"
     ```
   - Verify output: `BUILD SUCCESS` with all tests passing.
4. **Invalidation Conditions**:
   - If `RateLimitFilterIntegrationTest` passes without Docker, Docker was started.
   - If ByteBuddy attaches without `-javaagent`, JDK dynamic agent attachment restrictions were lifted or surefire was updated.
