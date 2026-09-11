# BRIEFING — 2026-09-10T10:41:35+07:00

## Mission
Investigate SecurityContext setup, user role management/transition (CUSTOMER -> VENDOR), Cloudinary/file storage integration, and security exception handling for Vendor Profile module.

## 🔒 My Identity
- Archetype: explorer
- Roles: security and storage investigator
- Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_explorer_survey_2
- Original parent: f6c3093b-5d45-4a16-b1ff-1fb079da1cc9
- Milestone: exploration

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Output handoff report to .agents/teamwork_preview_explorer_survey_2/handoff.md
- Send message back to caller f6c3093b-5d45-4a16-b1ff-1fb079da1cc9
- Communication must be in Vietnamese per user rule

## Current Parent
- Conversation ID: f6c3093b-5d45-4a16-b1ff-1fb079da1cc9
- Updated: not yet

## Investigation State
- **Explored paths**:
  - `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `JwtTokenProvider.java`
  - `AuthorizationPort.java`, `AuthorizationSubject.java`, `AuthorizationAdapter.java`, `AuthorizationController.java`
  - `AccountInternalApi.java`, `AccountInternalService.java`, `User.java`, `Role.java`, `UserMapper.java`
  - `pom.xml`, `application.yml`, `ApplicationBeans.java`, `Clean_Architecture_Rules.md`
  - `AuthenticationExceptionHandler.java`, `AuthorizationHandler.java`, `ErrorResponse.java`
  - `modules/vendor` domain & persistence files
- **Key findings**:
  1. Principal in SecurityContext is email (`String`), authorities have prefix `ROLE_` (`ROLE_CUSTOMER`, `ROLE_VENDOR`, `ROLE_ADMIN`).
  2. `userId` is obtained by looking up `AccountInternalApi.findUserByEmail(email)` or via a custom principal/SecurityUtils.
  3. Role transition: `user.setRole(Role.VENDOR)` via `accountInternalApi.saveUser(user)`. Real-time auth checks query DB per request so transition takes effect immediately.
  4. Cloudinary: No dependency or service exists in project yet. Need `DocumentStoragePort` in application layer and `CloudinaryStorageAdapter` in infrastructure.
  5. Error handling: Standard `ErrorResponse(code, message)` record used across module handlers. Need `VendorExceptionHandler` for 404 (VendorNotFoundException), 409 (VendorAlreadyExistsException), 403 (UserLockedException), 400 (InvalidDocTypeException).
- **Unexplored areas**: None. All 5 dispatch objectives investigated.

## Key Decisions Made
- Confirmed Clean Architecture port/adapter pattern for storage (`DocumentStoragePort`).
- Decided on controller-level extraction of `userId` from `SecurityContext` to pass into pure use cases.
- Identified need for explicit `VendorExceptionHandler` matching `ErrorResponse` schema.

## Artifact Index
- handoff.md — Final investigation report
- progress.md — Liveness and progress tracking
- BRIEFING.md — Working memory index
