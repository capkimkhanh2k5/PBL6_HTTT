# Progress: Milestone 2 (Presentation Layer & Security Integration)

- **Status**: COMPLETED
- **Last visited**: 2026-09-10T11:00:00+07:00

## Tasks
- [x] Review requirements, dispatch, M1 artifacts, and architecture contracts
- [x] Initialize BRIEFING.md and progress.md
- [x] Create DTOs:
  - [x] `RegisterVendorProfileRequest.java` (with `@NotBlank` constraints, `@JsonAlias` support, toCommand converter)
  - [x] `UpdateVendorProfileRequest.java` (strictly excludes status, rating, badge fields, `@JsonAlias` support)
  - [x] `VendorProfileResponse.java` (full vendor attributes, `fromDomain` factory)
  - [x] `VendorDocumentResponse.java` (document attributes, `fromDomain` factory)
- [x] Create Presentation Advice:
  - [x] `VendorExceptionHandler.java` (handles 409 Conflict, 404 Not Found, 403 Forbidden, 400 Bad Request)
- [x] Create Presentation Controller:
  - [x] `VendorProfileController.java` (endpoints for profile registration, retrieval, update, doc upload, doc retrieval; method security; authenticated userId extraction)
- [x] Verify build with `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw compile test-compile` (0 errors, BUILD SUCCESS)
- [ ] Write `handoff.md`
- [ ] Send completion message to parent
