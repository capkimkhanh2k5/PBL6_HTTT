# BRIEFING — 2026-09-10T04:19:00Z

## Mission
Investigate REST controllers, security contexts, DTOs, exception handling, and test infrastructure for Service Images in Milestone 2.

## 🔒 My Identity
- Archetype: explorer
- Roles: investigation, synthesis
- Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_m2_3
- Original parent: 10eadd78-2077-480b-a859-cfd0fcb5d524
- Milestone: Milestone 2 (Presentation & Testing)

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Response to user/caller must follow Vietnamese language rule
- Output handoff.md in working directory

## Current Parent
- Conversation ID: 10eadd78-2077-480b-a859-cfd0fcb5d524
- Updated: 2026-09-10T04:19:00Z

## Investigation State
- **Explored paths**:
  - `backend/src/main/java/com/danasea/backend/modules/service/presentation/`
  - `backend/src/main/java/com/danasea/backend/security/` (AuthenticationController, AuthorizationController, JwtAuthenticationFilter, SecurityConfig)
  - `backend/src/main/java/com/danasea/backend/shared/presentation/ErrorResponse.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/domain/exceptions/`
  - `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/`
  - `backend/src/test/java/com/danasea/backend/security/authentication/presentation/AuthenticationControllerTest.java`
  - `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- **Key findings**:
  - `VendorServiceImageController` will be created in `com.danasea.backend.modules.service.presentation.controllers`.
  - Security uses `@PreAuthorize("hasRole('VENDOR')")`. Principal email is extracted via `principal.getName()`, resolved to `userId` via `AccountInternalApi`.
  - Specs for 3 REST endpoints verified: POST upload, DELETE image, PATCH reorder.
  - DTO design: `ServiceImageResponse` record and `ReorderServiceImagesRequest` record.
  - Exception handling: `ServiceExceptionHandler` with `@RestControllerAdvice` mapping domain exceptions to `ErrorResponse` with standard HTTP codes (400, 403, 404).
  - Test infrastructure: JUnit 5, Mockito with `mock-maker-subclass`, standalone MockMvc for controller tests, and pure unit tests for use cases.
- **Unexplored areas**: None, all 5 target points thoroughly investigated.

## Key Decisions Made
- Outlined exact specifications, method signatures, DTO structures, exception mappings, and testing patterns for Milestone 2 presentation and testing.

## Artifact Index
- handoff.md — Final handoff report
- progress.md — Liveness heartbeat
- DISPATCH.md — Dispatched instructions log
