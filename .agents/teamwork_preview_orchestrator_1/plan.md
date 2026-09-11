# Implementation Plan: Vendor Profile Module

## 1. Objective
Implement the Vendor Profile module for Spring Boot according to `ORIGINAL_REQUEST.md`, including:
- Vendor registration (`POST /api/vendor/profile`)
- View vendor profile (`GET /api/vendor/profile`)
- Update vendor profile (`PATCH /api/vendor/profile`) with mass assignment protection
- Upload vendor document (`POST /api/vendor/documents`) to Cloudinary
- List vendor documents (`GET /api/vendor/documents`)
- Unit and Integration test suite:
  - `RegisterVendorProfileUseCaseTest`
  - `UpdateVendorProfileUseCaseTest`
  - `UploadVendorDocumentUseCaseTest`
  - `VendorProfileControllerTest`

## 2. Phases
### Phase 0: Survey & Discovery (Exploration)
- Explorer 1: Project structure, build tool (Maven vs Gradle), Spring Boot version, database/ORM (JPA/Flyway/Liquibase), entity relationships (User, Role, Vendor, VendorDocument).
- Explorer 2: Existing security context, authentication/authorization mechanism (JWT, UserDetails, current role transitions CUSTOMER -> VENDOR), Cloudinary service or file storage patterns.
- Explorer 3: Specification mining & test conventions, base test classes, MockMvc configuration, exception handling conventions (`VendorAlreadyExistsException`, etc.).

### Phase 1: Architecture & Milestone Definition
- Consolidate findings into `PROJECT.md`.
- Define milestones, contracts, and code layout.

### Phase 2: Implementation & Unit/Integration Tests
- Milestone execution via Worker and Reviewer subagents.
- E2E / Controller testing.

### Phase 3: Adversarial Challenge & Forensic Audit
- Verify test coverage and absence of bypasses / hardcoded mocks.
- Run full test suite (`mvn test` / `./gradlew test`).

### Phase 4: Final Handover & Victory Audit Notice
- Deliver final report to parent agent.
