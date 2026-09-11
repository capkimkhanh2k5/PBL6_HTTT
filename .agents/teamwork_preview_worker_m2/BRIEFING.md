# BRIEFING — 2026-09-10T11:00:00+07:00

## Mission
Implement Milestone 2 (Presentation Layer & Security Integration: DTOs, VendorProfileController, VendorExceptionHandler) for the Vendor Profile module cleanly following Clean Architecture and Spring Security.

## 🔒 My Identity
- Archetype: implementer
- Roles: implementer, qa
- Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_worker_m2
- Original parent: f6c3093b-5d45-4a16-b1ff-1fb079da1cc9
- Milestone: Milestone 2: Presentation Layer & Security Integration

## 🔒 Key Constraints
- Strictly follow Clean Architecture and Spring Security conventions.
- PATCH request DTO must strictly exclude `verification_status`, `rating_avg`, and `badge_tier`.
- Vendor identity derived strictly from SecurityContext (user ID), never path parameters or request bodies.
- Java 21 compilation verification required.
- Do not cheat, do not create dummy facades.

## Current Parent
- Conversation ID: f6c3093b-5d45-4a16-b1ff-1fb079da1cc9
- Updated: not yet

## Task Summary
- **What to build**:
  - DTOs: `RegisterVendorProfileRequest`, `UpdateVendorProfileRequest`, `VendorProfileResponse`, `VendorDocumentResponse`.
  - Controller: `VendorProfileController` with endpoints `/api/vendor/profile` (POST, GET, PATCH) and `/api/vendor/documents` (POST, GET).
  - Exception Handler: `VendorExceptionHandler` handling vendor and validation exceptions.
- **Success criteria**:
  - All DTOs and endpoints implemented accurately according to requirements.
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw compile test-compile` passes with 0 errors.
  - Handoff report written to `handoff.md`.
- **Interface contracts**: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_orchestrator_1/PROJECT.md`
- **Code layout**: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_profile_module/.agents/teamwork_preview_orchestrator_1/PROJECT.md § Code Layout`

## Key Decisions Made
- Implemented records for DTOs with Jackson `@JsonAlias` to accept both snake_case and camelCase parameters.
- Protected endpoints via `@PreAuthorize` matching Spring Security `ROLE_` convention (`ROLE_CUSTOMER`, `ROLE_VENDOR`).
- Derived authenticated identity in controller using `AccountInternalApi` lookup from Principal name.
- Implemented standard `@RestControllerAdvice` in `VendorExceptionHandler` mapping domain exceptions to RFC/HTTP status codes and standard `ErrorResponse`.

## Artifact Index
- `.agents/teamwork_preview_worker_m2/BRIEFING.md` — Agent briefing & memory
- `.agents/teamwork_preview_worker_m2/progress.md` — Liveness and task progress
- `.agents/teamwork_preview_worker_m2/handoff.md` — Final handoff report

## Change Tracker
- **Files modified**:
  - `backend/.../modules/vendor/presentation/dto/RegisterVendorProfileRequest.java`: Registration request DTO with validation annotations.
  - `backend/.../modules/vendor/presentation/dto/UpdateVendorProfileRequest.java`: Update request DTO strictly excluding status/rating/badge.
  - `backend/.../modules/vendor/presentation/dto/VendorProfileResponse.java`: Full vendor profile response DTO.
  - `backend/.../modules/vendor/presentation/dto/VendorDocumentResponse.java`: Vendor document response DTO.
  - `backend/.../modules/vendor/presentation/advice/VendorExceptionHandler.java`: Exception handler mapping domain exceptions to standard ErrorResponse.
  - `backend/.../modules/vendor/presentation/controller/VendorProfileController.java`: REST controller for vendor profile and document management.
- **Build status**: PASS (215 source files, 10 test files compiled with Java 21)
- **Pending issues**: None

## Quality Status
- **Build/test result**: PASS (`JAVA_HOME=... ./mvnw compile test-compile`)
- **Lint status**: 0 violations
- **Tests added/modified**: Ready for Milestone 3

## Loaded Skills
None
