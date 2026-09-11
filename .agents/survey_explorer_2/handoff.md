# Handoff Report — survey_explorer_2

**Role**: Security & Database Schema Explorer  
**Working Directory**: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/survey_explorer_2`  
**Target**: Spring Security & Database Schema for Categories Module  
**Date**: 2026-09-10  

---

## 1. Observation

1. **Security Configuration File & FilterChain**:
   - File path: `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`, lines 25–71.
   - Annotations: `@Configuration`, `@EnableWebSecurity`, `@EnableMethodSecurity`.
   - Lines 53–65 configure endpoint permissions:
     ```java
     .authorizeHttpRequests(auth -> auth
             .requestMatchers(
                     "/api/auth/login",
                     "/api/auth/register",
                     "/api/auth/refresh",
                     "/api/auth/logout",
                     "/actuator/health",
                     "/swagger-ui/**",
                     "/swagger-ui.html",
                     "/v3/api-docs/**"
             ).permitAll()
             .anyRequest()
             .authenticated())
     ```
   - Notice: Currently `/api/categories` is NOT in the `permitAll()` list, meaning it is protected by default under `.anyRequest().authenticated()`.

2. **Role Extraction & Mapping in Filter**:
   - File path: `backend/src/main/java/com/danasea/backend/security/authentication/infrastructure/security/JwtAuthenticationFilter.java`, lines 61–73:
     ```java
     List<SimpleGrantedAuthority> authorities = subject.roles()
         .stream()
         .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
         .toList();
     ```
   - Defined roles enum in `backend/src/main/java/com/danasea/backend/modules/account/domain/models/Role.java`:
     `CUSTOMER, VENDOR, ADMIN`
   - Existing controller usage in `backend/src/main/java/com/danasea/backend/security/authorization/presentation/AuthorizationController.java`, line 13:
     `@PreAuthorize("hasRole('ADMIN')")`

3. **Database Migration & JPA Configuration**:
   - `pom.xml` (lines 52–187): Contains `spring-boot-starter-data-jpa` and `postgresql` runtime. No `flyway-core` and no `liquibase-core`.
   - `application.yml` (lines 14–16):
     ```yaml
       jpa:
         hibernate:
           ddl-auto: update
     ```
   - Filesystem check for `.sql` files: 0 files found. Schema creation is entirely automated via Hibernate `ddl-auto: update`.

4. **Existing Category & Service Database Entities**:
   - File path: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/CategoryJpaEntity.java`:
     ```java
     @Entity
     @Getter
     @Setter
     @Table(name = "categorys")
     public class CategoryJpaEntity extends BaseJpaEntity {
         private String name;
         private String nameEn;
         private String slug;
         private UUID parentId;
         private String iconUrl;
         private Boolean isActive;
     }
     ```
   - File path: `backend/docs/DANASEA_Database_Design.docx` specifies:
     Table name is `categories` (with PK `id`, `name VARCHAR(100)`, `name_en VARCHAR(100)`, `slug VARCHAR(120) UNIQUE`, `parent_id UUID FK -> categories.id`, `icon_url TEXT`, `is_active BOOLEAN DEFAULT true`).
   - File path: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceJpaEntity.java`, line 20 and 47:
     `private UUID categoryId;`
     `@Enumerated(EnumType.STRING) private ServiceStatus status;`
   - File path: `backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceStatus.java`, line 4:
     `DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, PAUSED`

5. **Build & Test Environment Execution**:
   - JDK 21 located at `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`.
   - Command:
     ```bash
     export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
     ./mvnw test -Dtest=LoginUseCaseTest
     ```
     Result: BUILD SUCCESS, Tests run: 5, Failures: 0, Errors: 0. (Requires `BypassSandbox: true` due to ByteBuddy agent attachment).

---

## 2. Logic Chain

1. **Step 1 (Security Configuration for Categories)**:
   - Observation 1 shows that all requests not in the `requestMatchers(...).permitAll()` whitelist fall under `.anyRequest().authenticated()`.
   - Therefore, `GET /api/categories` will return 401/403 for public clients unless it is explicitly added to `requestMatchers` with `permitAll()`.
   - Observation 1 and Observation 2 demonstrate that Spring Security method security is active (`@EnableMethodSecurity`), and roles are mapped with `"ROLE_"` prefix.
   - For `/api/admin/categories/**`, pairing `.requestMatchers("/api/admin/**").hasRole("ADMIN")` in `SecurityConfig.java` with `@PreAuthorize("hasRole('ADMIN')")` on `AdminCategoryController` provides defense-in-depth authorization matching codebase standards.

2. **Step 2 (Database Schema Management)**:
   - Observation 3 shows that neither Flyway nor Liquibase is installed or configured in `pom.xml`, and no migration scripts exist.
   - Hibernate's `ddl-auto: update` manages schema modifications.
   - Therefore, modifying or creating JPA entities (`@Entity`, `@Table`, `@Column`) directly dictates the PostgreSQL database schema.

3. **Step 3 (Categories & Services Schema Alignment)**:
   - Observation 4 shows that `CategoryJpaEntity` currently has `@Table(name = "categorys")`, whereas `DANASEA_Database_Design.docx` designates `categories`. Updating the annotation to `@Table(name = "categories")` aligns the entity with the design specification.
   - Observation 4 also shows that `ServiceJpaEntity` tracks categories via `UUID categoryId` and lifecycle state via `ServiceStatus status`.
   - For deactivation enforcement (R1 & Acceptance Criteria: reject deactivation if category has active services), checking `existsByCategoryIdAndStatus(categoryId, ServiceStatus.PUBLISHED)` in `JpaServiceRepository` directly satisfies the domain rule without foreign key cascading issues.

---

## 3. Caveats

1. **Table name refactoring ("categorys" vs "categories")**:
   If an existing test database instance has already created a `categorys` table, renaming the entity table annotation to `categories` will create a new table `categories` under `ddl-auto: update`. Since there are no migration files and development is active, this is safe and desirable.
2. **Active service statuses**:
   `ServiceStatus.PUBLISHED` is the primary active status in the marketplace. If business requirements later consider `PENDING_REVIEW` or `PAUSED` as blocking deactivation, using `existsByCategoryIdAndStatusIn(UUID, Collection<ServiceStatus>)` provides a future-proof method signature.
3. **Sandbox execution**:
   Running `./mvnw test` inside macOS sandbox fails on Mockito dynamic agent attachment. Test execution must bypass the sandbox or include proper JVM parameters (`-XX:+EnableDynamicAgentLoading`).

---

## 4. Conclusion

1. **Security**:
   - Add `requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()` to `SecurityFilterChain` in `SecurityConfig.java`.
   - Add `requestMatchers("/api/admin/**").hasRole("ADMIN")` to `SecurityConfig.java` and annotate `AdminCategoryController` with `@PreAuthorize("hasRole('ADMIN')")`.
2. **Database**:
   - No migration scripts needed (Flyway/Liquibase absent). Hibernate `ddl-auto: update` is used.
   - Fix `CategoryJpaEntity` `@Table(name = "categories")` and add field constraints (`unique = true` on `slug`, `@Column(name = "is_active")`, etc.).
   - Add active service query method `boolean existsByCategoryIdAndStatus(UUID categoryId, ServiceStatus status)` to `JpaServiceRepository`.

---

## 5. Verification Method

1. **Verify Security Configuration**:
   - Inspect `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`.
   - Run MockMvc controller tests for public categories endpoint without auth token -> expects 200 OK.
   - Run MockMvc controller tests for admin categories endpoints with customer role -> expects 403 Forbidden; with admin role -> expects 200/201 OK.
2. **Verify Tests Execution**:
   - Run the project test command:
     ```bash
     export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
     ./mvnw test -Dtest=CreateCategoryUseCaseTest,GetCategoryTreeUseCaseTest,DeactivateCategoryUseCaseTest
     ```
3. **Invalidation Conditions**:
   - If Flyway or Liquibase is introduced to `pom.xml`, schema updates must switch from JPA entities to migration scripts.
   - If `JwtAuthenticationFilter` role prefixing is modified, `@PreAuthorize("hasRole('ADMIN')")` evaluation will change.
