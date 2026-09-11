## 2026-09-10T03:50:24Z
You are Worker for Milestone 1: Domain, Persistence & Security Foundations.

Your Working Directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/.agents/teamwork_preview_worker_m1
Parent Conversation ID: adb2e576-1356-4e14-adfc-71974a3fd054

MANDATORY FIRST STEPS:
1. Read ORIGINAL_REQUEST.md at:
   /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/ORIGINAL_REQUEST.md
2. Read PROJECT.md at:
   /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/PROJECT.md
3. Read the 3 Survey Reports:
   - /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/.agents/teamwork_preview_explorer_survey_1/handoff.md
   - /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/.agents/teamwork_preview_explorer_survey_2/handoff.md
   - /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/.agents/teamwork_preview_explorer_survey_3/handoff.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Scope & Exclusive File Ownership for Milestone 1:
You exclusively own and implement:
1. `backend/src/main/java/com/danasea/backend/modules/service/domain/exceptions/ServiceNotFoundException.java`
2. `backend/src/main/java/com/danasea/backend/modules/service/domain/ports/ServiceRepositoryPort.java`
3. `backend/src/main/java/com/danasea/backend/modules/service/domain/ports/WishlistRepositoryPort.java`
4. `backend/src/main/java/com/danasea/backend/modules/service/domain/ports/RecentlyViewedRepositoryPort.java`
5. `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceRepository.java`
   - Add `@Modifying @Query("UPDATE ServiceJpaEntity s SET s.viewCount = COALESCE(s.viewCount, 0) + 1 WHERE s.id = :id AND s.status = :status") int incrementViewCount(@Param("id") UUID id, @Param("status") ServiceStatus status);`
   - Extend `JpaSpecificationExecutor<ServiceJpaEntity>`
6. `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaWishlistRepository.java`
   - Add `boolean existsByUserIdAndServiceId(UUID userId, UUID serviceId);`
   - Add `void deleteByUserIdAndServiceId(UUID userId, UUID serviceId);`
   - Add `List<WishlistJpaEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);`
7. `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaRecentlyViewedRepository.java`
   - Add `Optional<RecentlyViewedJpaEntity> findFirstByUserIdAndServiceId(UUID userId, UUID serviceId);`
   - Add `Optional<RecentlyViewedJpaEntity> findFirstBySessionIdAndServiceId(String sessionId, UUID serviceId);`
   - Add `List<RecentlyViewedJpaEntity> findAllByUserIdOrderByViewedAtDesc(UUID userId);`
   - Add `List<RecentlyViewedJpaEntity> findAllBySessionIdOrderByViewedAtDesc(String sessionId);`
8. `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/specifications/ServiceSpecifications.java`
   - Dynamic Specification combining:
     - `status = ServiceStatus.PUBLISHED` (MANDATORY in all queries)
     - `categoryId` equals (if present)
     - `keyword` case-insensitive match on name, nameEn, description
     - `minPrice` / `maxPrice` range
     - `lat`, `lng`, `radiusKm` location bounding box / distance filter
9. Mappers:
   - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/mapper/ServiceMapper.java`
   - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/mapper/WishlistMapper.java`
   - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/mapper/RecentlyViewedMapper.java`
10. Adapters implementing ports:
   - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/adapters/ServiceRepositoryAdapter.java`
   - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/adapters/WishlistRepositoryAdapter.java`
   - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/adapters/RecentlyViewedRepositoryAdapter.java`
11. Security Configuration updates:
   - `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`:
     - Permit `HttpMethod.GET, "/api/services", "/api/services/**"` and `HttpMethod.GET, "/api/recently-viewed"` in `authorizeHttpRequests`
     - Add `authenticationEntryPoint` returning HTTP 401 with `{"code":"UNAUTHORIZED","message":"Authentication required"}`
   - `backend/src/main/java/com/danasea/backend/security/authentication/infrastructure/security/JwtAuthenticationFilter.java`:
     - In `authenticate(AuthorizationSubject subject)`: set `authentication.setDetails(subject);`
   - `backend/src/main/java/com/danasea/backend/security/infrastructure/SecurityUtils.java`:
     - Provide `public static Optional<UUID> getCurrentUserId()`

Verification Requirements:
- Build and verify via commands:
  ```bash
  export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
  export PATH=$JAVA_HOME/bin:$PATH
  cd /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/backend
  ./mvnw test-compile
  ./mvnw test -Dtest=AuthenticationControllerTest
  ```
- Write detailed report in `handoff.md` at your working directory.
- Send a message to parent with completion status when done.
