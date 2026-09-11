# BRIEFING — 2026-09-10T03:47:00Z

## Mission
Survey security configuration, API patterns, controller conventions, authentication/guest context handling, and endpoint specifications for Public Catalog, Wishlist, and Recently Viewed features.

## 🔒 My Identity
- Archetype: Explorer
- Roles: Security & API Patterns Survey Specialist
- Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/.agents/teamwork_preview_explorer_survey_2
- Original parent: adb2e576-1356-4e14-adfc-71974a3fd054
- Milestone: Survey Phase

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Investigate Spring Security, JWT, session, filters, public vs authenticated endpoint rules
- Investigate user_id and session_id extraction mechanism
- Investigate existing controllers, REST conventions, response wrappers, exception handling
- Define exact endpoint requirements and authorization constraints for R1, R2, R3

## Current Parent
- Conversation ID: adb2e576-1356-4e14-adfc-71974a3fd054
- Updated: 2026-09-10T03:47:00Z

## Investigation State
- **Explored paths**: 
  - `ORIGINAL_REQUEST.md`
  - `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`
  - `backend/src/main/java/com/danasea/backend/config/ApplicationBeans.java`
  - `backend/src/main/java/com/danasea/backend/security/...`
  - `backend/src/main/java/com/danasea/backend/modules/service/...`
  - `backend/src/main/java/com/danasea/backend/modules/account/...`
  - `backend/src/main/java/com/danasea/backend/shared/...`
  - `backend/src/test/...`
- **Key findings**:
  - `SecurityConfig` is stateless JWT-based; currently only `/api/auth/**`, actuator health, and swagger docs are permitted. All others require authentication.
  - For R1, `GET /api/services` and `GET /api/services/{id}` must be added to `permitAll()`.
  - For R3, `GET /api/recently-viewed` must also be public (`permitAll()`) so guests can retrieve recently viewed history.
  - `JwtAuthenticationFilter` resolves `AuthorizationSubject` (containing `userId`, `email`, `roles`, `permissions`), but sets only `subject.email()` as principal. Setting `authentication.setDetails(subject)` or using a context helper allows clean extraction of `userId`.
  - `session_id` (guest) is not yet supported in controllers/filters; recommended convention is `X-Session-Id` header (with query param fallback).
  - Clean Architecture modular pattern: Controllers (`presentation`) call Use Cases (`application`) which use Ports (`domain/repositories`) implemented by Adapters (`infrastructure/persistence`). Use cases are configured as `@Bean`s in config classes.
  - Standard error format is `ErrorResponse(String code, String message)` handled via `@RestControllerAdvice`.
  - Concurrency for `view_count` increment on `GET /api/services/{id}` should use an atomic SQL update `@Modifying @Query` in `JpaServiceRepository`.
  - Upsert on `recently_vieweds` should be scoped by `(userId, serviceId)` or `(sessionId, serviceId)` with timestamp update to prevent duplicate records.
- **Unexplored areas**: None within survey scope.

## Key Decisions Made
- Fully documented SecurityConfig rules, user/session ID extraction pattern, Clean Architecture controller conventions, DTO standards, and endpoint requirements.

## Artifact Index
- handoff.md — Comprehensive survey report
- progress.md — Liveness and status heartbeat
- DISPATCH.md — Initial task dispatch record
