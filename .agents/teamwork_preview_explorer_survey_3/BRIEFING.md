# BRIEFING — 2026-09-10T10:46:00+07:00

## Mission
Survey Testing & Edge Cases for Public Catalog, Wishlist, and Recently Viewed features.

## 🔒 My Identity
- Archetype: explorer
- Roles: [Testing & Edge Cases Explorer]
- Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/.agents/teamwork_preview_explorer_survey_3
- Original parent: adb2e576-1356-4e14-adfc-71974a3fd054
- Milestone: survey

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Explore test setup, existing tests, commands, test execution
- Investigate R4 required tests and specific edge cases
- Write findings to handoff.md and notify parent

## Current Parent
- Conversation ID: adb2e576-1356-4e14-adfc-71974a3fd054
- Updated: 2026-09-10T10:46:00+07:00

## Investigation State
- **Explored paths**:
  - `ORIGINAL_REQUEST.md`
  - `backend/pom.xml`
  - `backend/src/test/java/com/danasea/backend/BackendApplicationTests.java`
  - `backend/src/test/java/com/danasea/backend/security/authentication/presentation/filter/RateLimitFilterIntegrationTest.java`
  - `backend/src/test/java/com/danasea/backend/security/authentication/presentation/AuthenticationControllerTest.java`
  - `backend/src/test/java/com/danasea/backend/security/authentication/application/usecase/LoginUseCaseTest.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/**` (domain models, entities, repositories)
  - `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`
  - `backend/src/main/java/com/danasea/backend/shared/**`
  - `backend/docs/Clean_Architecture_Rules.md`
- **Key findings**:
  - Build tool: Maven with `./mvnw` (Maven 3.9.16) and Java 21 (Eclipse Adoptium Temurin-21.0.10).
  - Test framework: JUnit 5, Mockito 5.23.0, MockMvc, Spring Boot Test, Testcontainers.
  - Critical discovery: Running `./mvnw test` fails out-of-the-box due to:
    1. Java 21 ByteBuddy dynamic agent attachment failure with Mockito inline mock maker. Fix verified: `-javaagent:.../byte-buddy-agent-1.18.11.jar` or configuring surefire / `mock-maker-subclass`.
    2. `RateLimitFilterIntegrationTest` requires running Docker.
    When running unit tests excluding Docker integration test with the agent flag, all 27 unit tests pass cleanly in ~4 seconds.
  - Current service module has models and JPA entities/repositories, but NO application use cases or controllers implemented yet.
  - Required tests identified for R4: `SearchServicesUseCaseTest`, `GetServiceDetailUseCaseTest`, `RecordRecentlyViewedUseCaseTest`, `WishlistUseCaseTest`, `CatalogControllerTest`.
  - Concrete mitigation strategies identified for all 4 key edge cases (concurrency on `view_count`, idempotency on wishlist, 404 for draft/non-PUBLISHED, guest/user recently viewed upserting).
- **Unexplored areas**: None for the survey phase.

## Key Decisions Made
- Survey completed. Preparing comprehensive handoff.md.

## Artifact Index
- handoff.md — Comprehensive Testing & Edge Cases Survey Report
