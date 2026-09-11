# Handoff Report — Explorer 2: Security & API Patterns Survey

## 1. Observation

### 1.1 Security Configuration (`SecurityConfig.java`)
- **File**: `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java` (lines 38-70)
- **Current Rules**:
  ```java
  .cors(cors -> cors.configurationSource(corsConfigurationSource()))
  .csrf(csrf -> csrf.disable())
  .sessionManagement(session ->
          session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
  .exceptionHandling(exception -> exception
          .accessDeniedHandler((request, response, accessDenied) -> {
              response.setStatus(HttpServletResponse.SC_FORBIDDEN);
              response.setContentType(MediaType.APPLICATION_JSON_VALUE);
              response.getWriter().write(
                      "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access denied\"}"
              );
          }))
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
  .addFilterBefore(
          jwtAuthenticationFilter,
          UsernamePasswordAuthenticationFilter.class
  )
  ```
- **Observations**:
  1. Session management is strictly `STATELESS`. CSRF is disabled.
  2. Filter chain applies `JwtAuthenticationFilter` prior to `UsernamePasswordAuthenticationFilter`.
  3. Only `/api/auth/**`, `/actuator/health`, and Swagger endpoints are currently `permitAll()`. All other endpoints require authentication (`anyRequest().authenticated()`).
  4. There is an `accessDeniedHandler` returning HTTP 403 with `{"code":"ACCESS_DENIED","message":"Access denied"}`, but **no custom `authenticationEntryPoint`** is configured. If an unauthenticated user calls an authenticated endpoint, Spring Security falls back to its default behavior instead of returning a standardized JSON error response (e.g. `{"code":"UNAUTHORIZED","message":"Authentication required"}`).

### 1.2 JWT & User Authentication Flow (`JwtAuthenticationFilter.java` & `AuthorizationSubject.java`)
- **File**: `backend/src/main/java/com/danasea/backend/security/authentication/infrastructure/security/JwtAuthenticationFilter.java` (lines 38-82)
  ```java
  String header = request.getHeader("Authorization");
  if (header == null || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
  }
  String token = header.substring(7);
  if (SecurityContextHolder.getContext().getAuthentication() == null) {
      tokenProvider.getEmail(token)
          .flatMap(userAccountPort::findByEmail)
          .filter(user -> user.enabled())
          .map(user -> authorizationPort.findSubjectByEmail(user.email()))
          .filter(subject -> !subject.roles().isEmpty())
          .ifPresent(this::authenticate);
  }
  ```
  ```java
  private void authenticate(AuthorizationSubject subject) {
      List<SimpleGrantedAuthority> authorities = ...;
      var authentication = new UsernamePasswordAuthenticationToken(
          subject.email(),
          null,
          authorities
      );
      SecurityContextHolder.getContext().setAuthentication(authentication);
  }
  ```
- **File**: `backend/src/main/java/com/danasea/backend/security/authorization/domain/model/AuthorizationSubject.java` (lines 6-11)
  ```java
  public record AuthorizationSubject (
      UUID userId,
      String email,
      Set<String> roles,
      Set<String> permissions
  )
  ```
- **Observations**:
  1. `JwtAuthenticationFilter` extracts the email from the JWT claims and resolves an `AuthorizationSubject`.
  2. Notice that `AuthorizationSubject` **already contains `userId` (`UUID`)**!
  3. However, `authenticate()` currently passes `subject.email()` as the principal to `UsernamePasswordAuthenticationToken(subject.email(), null, authorities)` and does **not** attach `subject` to `authentication.setDetails(...)`.
  4. As a result, controllers currently accessing `Principal.getName()` only obtain the user's email string.

### 1.3 Guest Session Handling
- **Files**:
  - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/RecentlyViewed.java` (line 14): `private String sessionId;`
  - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/RecentlyViewedJpaEntity.java` (line 19): `private String sessionId;`
- **Observations**:
  1. `sessionId` is already modeled in `RecentlyViewed` and `RecentlyViewedJpaEntity`.
  2. However, there is no filter, interceptor, or resolver in the codebase that parses or populates `sessionId`.
  3. No frontend or mobile client has pre-existing hardcoded session header requirements; the standard REST convention for SPA/mobile is the HTTP header `X-Session-Id` (with optional fallback to request param/cookie `sessionId`).

### 1.4 Existing Controllers, DTOs & Exception Handling Patterns
- **Controllers**:
  - `backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationController.java`
  - `backend/src/main/java/com/danasea/backend/security/authorization/presentation/AuthorizationController.java`
- **Architecture**:
  - Controllers are annotated with `@RestController`, `@RequestMapping("/api/...")`, `@RequiredArgsConstructor`.
  - Controllers inject and delegate directly to Application Use Cases (e.g. `LoginUseCase`, `RegisterUseCase`).
  - Use cases are pure Java classes (Clean Architecture Rule 7: Framework Independence) instantiated as `@Bean`s in configuration classes (e.g. `backend/src/main/java/com/danasea/backend/config/ApplicationBeans.java`).
- **DTOs & Records**:
  - Java `record`s are consistently used for all request/response DTOs (e.g. `LoginRequest`, `AuthenticationResponse`, `RefreshResponse`).
  - Validation uses `jakarta.validation.Valid` on `@RequestBody` with constraint annotations (`@NotBlank`, `@NotNull`, etc.).
- **Standard Error Format**:
  - `backend/src/main/java/com/danasea/backend/shared/presentation/ErrorResponse.java`:
    ```java
    public record ErrorResponse(
            String code,
            String message
    ) {}
    ```
  - Exception handling via `@RestControllerAdvice` (e.g. `AuthenticationExceptionHandler.java` and `AuthorizationHandler.java`), returning `ResponseEntity<ErrorResponse>` with HTTP status codes:
    - 400 Bad Request: `INVALID_INPUT`, `INVALID_OTP`
    - 401 Unauthorized: `INVALID_CREDENTIALS`
    - 403 Forbidden: `ACCESS_DENIED`
    - 409 Conflict: `EMAIL_ALREADY_USED`

### 1.5 Existing Service Module State
- **Files under `backend/src/main/java/com/danasea/backend/modules/service/`**:
  - Domain models exist: `Service`, `ServiceStatus` (`DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, PAUSED`), `Category`, `ServiceImage`, `ServiceSlot`, `ServiceSafetyDocument`, `Wishlist`, `RecentlyViewed`.
  - JPA Entities exist: `ServiceJpaEntity`, `CategoryJpaEntity`, `ServiceImageJpaEntity`, `WishlistJpaEntity`, `RecentlyViewedJpaEntity`, etc.
  - Basic Spring Data repositories exist: `JpaServiceRepository`, `JpaCategoryRepository`, `JpaWishlistRepository`, `JpaRecentlyViewedRepository`, `JpaServiceImageRepository`.
  - **Missing**: Domain repository port interfaces, application use cases, mappers, persistence adapters, and REST controllers.

---

## 2. Logic Chain

### 2.1 Endpoint Authorization & Security Configuration
- **Step 1 (Public Catalog)**:
  `ORIGINAL_REQUEST.md` lines 18-20 require `GET /api/services` and `GET /api/services/{id}` to be public.
  In `SecurityConfig.java`, currently only `/api/auth/**` is public; `.anyRequest().authenticated()` blocks all others.
  Therefore, `SecurityConfig` must explicitly permit `GET /api/services` and `GET /api/services/**`.
- **Step 2 (Recently Viewed Dual Access)**:
  `ORIGINAL_REQUEST.md` line 28 states `GET /api/recently-viewed` must "Retrieve the user's or guest's recently viewed history."
  Because guests are unauthenticated, `GET /api/recently-viewed` cannot require authentication at the Spring Security filter chain level. It must be added to `permitAll()`. The controller will inspect authentication dynamically: if authenticated, it uses `userId`; if unauthenticated, it uses `sessionId`.
- **Step 3 (Wishlist Protection)**:
  `ORIGINAL_REQUEST.md` lines 22-25 define Wishlist endpoints (`POST /api/wishlists/{serviceId}`, `DELETE /api/wishlists/{serviceId}`, `GET /api/wishlists`).
  Wishlists belong strictly to a logged-in user (`WishlistJpaEntity.userId`). These endpoints must remain protected by `.anyRequest().authenticated()`, returning 401/403 when no valid Bearer token is supplied.
- **Step 4 (Authentication Entry Point)**:
  Currently, `SecurityConfig` lacks an `AuthenticationEntryPoint`. When unauthenticated requests hit `/api/wishlists/**`, Spring Security defaults to 403 or HTML redirect. Adding a custom `AuthenticationEntryPoint` returning `{"code":"UNAUTHORIZED","message":"Authentication required"}` (401) ensures consistent REST response format.

### 2.2 Extraction of `userId` and `sessionId`
- **Step 1 (`userId`)**:
  In `JwtAuthenticationFilter.java`, `subject` is an `AuthorizationSubject` with `subject.userId()`. If we store `subject` in `authentication.setDetails(subject)`, then any controller or service can obtain the authenticated `userId` in O(1) time without extra database queries:
  ```java
  var authentication = new UsernamePasswordAuthenticationToken(subject.email(), null, authorities);
  authentication.setDetails(subject);
  ```
  A utility method `SecurityUtils.getCurrentUserId()` can check `SecurityContextHolder.getContext().getAuthentication()`:
  - If details contain `AuthorizationSubject`, return `Optional.of(subject.userId())`.
  - Fallback: If details are not present, lookup by `authentication.getName()` using `AccountInternalApi.findUserByEmail(email)`.
- **Step 2 (`sessionId`)**:
  For guest requests, the client passes `X-Session-Id: <uuid_or_string>` header.
  The controller receives this via `@RequestHeader(value = "X-Session-Id", required = false) String sessionId`.
  If user is authenticated, `userId` is used; if unauthenticated, `sessionId` is used.

### 2.3 Concurrency & Race Condition on `view_count`
- **Step 1**:
  `ORIGINAL_REQUEST.md` line 37 requires: "`GET /api/services/{id}` handles concurrent view count increments safely (e.g., atomic updates)."
- **Step 2**:
  Reading `ServiceJpaEntity`, modifying `viewCount` in memory, and calling `save()` causes lost updates under concurrent traffic.
- **Step 3**:
  Adding an atomic DB update query in `JpaServiceRepository`:
  ```java
  @Modifying
  @Query("UPDATE ServiceJpaEntity s SET s.viewCount = COALESCE(s.viewCount, 0) + 1 WHERE s.id = :id AND s.status = 'PUBLISHED'")
  int incrementViewCount(@Param("id") UUID id);
  ```
  guarantees atomic row-level increment in PostgreSQL without race conditions and without optimistic locking rollback overhead.

### 2.4 Idempotency & Upsert in Recently Viewed and Wishlist
- **Step 1 (Recently Viewed Upsert)**:
  `RecentlyViewedJpaEntity` tracks `(userId, sessionId, serviceId, viewedAt)`.
  When recording:
  - For user: check `findByUserIdAndServiceId(userId, serviceId)`. If present, update `viewedAt = OffsetDateTime.now()`; if absent, insert new entity.
  - For guest: check `findBySessionIdAndServiceId(sessionId, serviceId)`. If present, update `viewedAt = OffsetDateTime.now()`; if absent, insert new entity.
  This satisfies acceptance criteria: "updating timestamps without duplicating records".
- **Step 2 (Wishlist Idempotency)**:
  - `POST /api/wishlists/{serviceId}`: check `existsByUserIdAndServiceId(userId, serviceId)`. If true, return 200 OK (idempotent, no duplicate rows). If false, verify service exists & is `PUBLISHED`, then insert.
  - `DELETE /api/wishlists/{serviceId}`: check if exists; if exists, delete; if not, return 200 OK or 204 No Content gracefully without 404 or 500 error.

### 2.5 Catalog Filter Specification (`GET /api/services`)
- **Step 1**:
  `ORIGINAL_REQUEST.md` line 19 & 36: Filter by category, keyword, location (lat/lng/radius), price range. Must only return `PUBLISHED` services.
- **Step 2**:
  In `JpaServiceRepository`, implement using Spring Data JPA `JpaSpecificationExecutor<ServiceJpaEntity>`:
  - Predicate 1 (Mandatory): `status = ServiceStatus.PUBLISHED`.
  - Predicate 2 (Category): `categoryId = :categoryId` (if present).
  - Predicate 3 (Keyword): `LOWER(name) LIKE %keyword% OR LOWER(nameEn) LIKE %keyword% OR LOWER(description) LIKE %keyword%`.
  - Predicate 4 (Price range): `price >= minPrice` and `price <= maxPrice`.
  - Predicate 5 (Location): bounding box or Haversine distance formula `d <= radiusKm`.

---

## 3. Caveats
1. **SecurityConfig Location & Packaging**:
   `SecurityConfig.java` is located at `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`, but has `package com.danasea.backend.modules.systemconfig;`. This works because `BackendApplication` is at `com.danasea.backend`, scanning all subpackages.
2. **MockMaker in JDK 21 on macOS**:
   Executing tests directly inside the sandboxed environment without `BypassSandbox` fails Mockito dynamic ByteBuddy agent attachment (`Could not initialize inline Byte Buddy mock maker`). With `BypassSandbox: true`, tests run and pass cleanly (confirmed with `AuthenticationControllerTest`).
3. **Session ID Format**:
   `RecentlyViewedJpaEntity.sessionId` is a `String`. We assume any string (e.g. standard UUID string generated by client or server) is acceptable as `sessionId`. If no session ID is supplied by a guest on `GET /api/services/{id}`, the view count is still incremented, but recently viewed history is only stored if a `sessionId` is present.
4. **Geo-location Radius Filter**:
   PostgreSQL without PostGIS can calculate distance using the spherical law of cosines or Haversine formula in JPA specification or native SQL:
   `6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(latitude))) <= :radiusKm`.
   A simpler bounding box filter (`abs(latitude - :lat) <= deltaLat` and `abs(longitude - :lng) <= deltaLng`) can also be combined for query index acceleration.

---

## 4. Conclusion & Actionable Recommendations

### 4.1 SecurityConfig Adjustments
In `SecurityConfig.java`, update `authorizeHttpRequests`:
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
        .requestMatchers(HttpMethod.GET, "/api/services", "/api/services/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/recently-viewed").permitAll()
        .anyRequest()
        .authenticated())
```
Add `.authenticationEntryPoint`:
```java
.exceptionHandling(exception -> exception
        .authenticationEntryPoint((request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}"
            );
        })
        .accessDeniedHandler((request, response, accessDenied) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access denied\"}"
            );
        }))
```

### 4.2 SecurityUtils & Authentication Details
1. In `JwtAuthenticationFilter.java:80`:
   ```java
   var authentication = new UsernamePasswordAuthenticationToken(
       subject.email(),
       null,
       authorities
   );
   authentication.setDetails(subject);
   SecurityContextHolder.getContext().setAuthentication(authentication);
   ```
2. Create `SecurityUtils.java` in `shared` or `security`:
   ```java
   public final class SecurityUtils {
       public static Optional<UUID> getCurrentUserId() {
           Authentication auth = SecurityContextHolder.getContext().getAuthentication();
           if (auth != null && auth.isAuthenticated() && auth.getDetails() instanceof AuthorizationSubject subject) {
               return Optional.ofNullable(subject.userId());
           }
           return Optional.empty();
       }
   }
   ```

### 4.3 Clean Architecture Structure for Implementation
Under `backend/src/main/java/com/danasea/backend/modules/service/`:
1. **Domain Layer**:
   - `domain/exceptions/ServiceNotFoundException.java`
   - `domain/repositories/ServiceRepository.java`
   - `domain/repositories/WishlistRepository.java`
   - `domain/repositories/RecentlyViewedRepository.java`
2. **Application Layer**:
   - `application/usecase/SearchServicesUseCase.java`
   - `application/usecase/GetServiceDetailUseCase.java`
   - `application/usecase/RecordRecentlyViewedUseCase.java`
   - `application/usecase/WishlistUseCase.java` (or add/remove/list methods)
   - `application/dto/ServiceSearchCriteria.java`, `ServiceSummaryResult.java`, `ServiceDetailResult.java`, `WishlistItemResult.java`, `RecentlyViewedResult.java`
3. **Infrastructure Layer**:
   - `infrastructure/persistence/adapters/ServiceRepositoryAdapter.java`
   - `infrastructure/persistence/adapters/WishlistRepositoryAdapter.java`
   - `infrastructure/persistence/adapters/RecentlyViewedRepositoryAdapter.java`
   - `infrastructure/mapper/ServiceMapper.java`, `WishlistMapper.java`, `RecentlyViewedMapper.java`
   - Atomic update method in `JpaServiceRepository`: `incrementViewCount(UUID id)`
4. **Presentation Layer**:
   - `presentation/CatalogController.java` (`/api/services`, `/api/recently-viewed`)
   - `presentation/WishlistController.java` (`/api/wishlists`)
   - `presentation/CatalogExceptionHandler.java` (handles `ServiceNotFoundException` -> 404 `SERVICE_NOT_FOUND`)
   - Presentation response records: `ServiceSummaryResponse`, `ServiceDetailResponse`, `WishlistItemResponse`, `RecentlyViewedResponse`
5. **Config Beans**:
   - Add beans to `ApplicationBeans.java` (or a dedicated `ServiceBeans.java` configuration class) to instantiate the use cases cleanly.

### 4.4 Automated Tests (R4)
- **Unit Tests**:
  - `SearchServicesUseCaseTest` (verifies PUBLISHED-only filtering, keywords, price, category)
  - `GetServiceDetailUseCaseTest` (verifies 404 for non-published services, invokes atomic view increment & recently viewed recording)
  - `RecordRecentlyViewedUseCaseTest` (verifies user upsert vs guest session upsert, timestamp update)
  - `WishlistUseCaseTest` (verifies idempotent additions and deletions, validation)
- **Controller & Security Tests**:
  - `CatalogControllerTest` using `MockMvc`:
    - Public access to `GET /api/services` and `GET /api/services/{id}` without token (200 OK).
    - Unauthenticated access to `POST /api/wishlists/{serviceId}`, `DELETE /api/wishlists/{serviceId}`, `GET /api/wishlists` returns 401/403.
    - Authenticated access with valid JWT token succeeds (200 OK / 201 Created).
    - `GET /api/recently-viewed` returns 200 OK for guest with `X-Session-Id` and for authenticated user with token.

---

## 5. Verification Method

### 5.1 Verification Commands
1. **Compilation Check**:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   ./mvnw test-compile
   ```
2. **Existing Tests Execution**:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   ./mvnw test -Dtest=AuthenticationControllerTest
   ```
   (Must exit code 0).
3. **New Module Tests Execution (Post-Implementation)**:
   ```bash
   ./mvnw test -Dtest="*UseCaseTest,*ControllerTest"
   ```

### 5.2 Key Inspection Points
- Verify `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java` has `/api/services/**` and `/api/recently-viewed` permitted under `GET`.
- Verify `GET /api/services` query generates SQL with `WHERE status = 'PUBLISHED'`.
- Verify `incrementViewCount` executes `UPDATE services SET view_count = view_count + 1 WHERE id = ?`.
- Verify `GET /api/services/{draft_id}` returns 404 Not Found with `{"code":"SERVICE_NOT_FOUND", ...}`.
- Verify `POST /api/wishlists/{serviceId}` without `Authorization` header returns 401 or 403.

### 5.3 Invalidation Conditions
- If `SecurityConfig` allows non-GET methods on `/api/services` without authentication (e.g. POST/PUT by vendors), public write access would be exposed.
- If `view_count` increment is implemented as `service.setViewCount(service.getViewCount() + 1)` in Java memory, concurrent view increment will suffer race conditions under load.
- If `JwtAuthenticationFilter` is modified in a way that changes `principal.getName()` from returning email, `AuthenticationControllerTest` or existing services could break. Storing `subject` in `authentication.setDetails(subject)` avoids breaking changes.
