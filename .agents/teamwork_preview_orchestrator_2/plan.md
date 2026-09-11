# Execution Plan — Milestone 2 & Milestone 3 (Generation 2)

## Context & Objectives
Resuming from Milestone 1 completion. Milestone 1 successfully established domain repository ports, JPA queries, specification, mappers, adapters, and security configurations (72 tests passing).

## Milestone 2: Application Use Cases & REST Controllers
1. **Phase 2A — Technical Investigation (Explorers)**:
   - Dispatch 3 Explorers in parallel to inspect:
     - Explorer 1: Domain models, port interfaces, and DTO requirements for Use Cases (`SearchServicesUseCase`, `GetServiceDetailUseCase`, `RecordRecentlyViewedUseCase`, `WishlistUseCase`, `GetRecentlyViewedUseCase`).
     - Explorer 2: REST Controllers (`CatalogController`, `WishlistController`, `RecentlyViewedController`), Request/Response DTOs, endpoint paths, HTTP status codes, security integration (`SecurityUtils`), header extraction (`X-Session-Id`).
     - Explorer 3: Global Exception Handlers (`CatalogExceptionHandler`, `ServiceNotFoundException` mapping to 404), Spring bean configuration (`ServiceBeans`).
2. **Phase 2B — Implementation (Worker)**:
   - Dispatch Worker with consolidated Explorer findings, strict code boundaries, and anti-cheating integrity warning.
   - Implement all use cases, DTOs, controllers, exception handlers, and bean definitions.
   - Run compilation (`./mvnw test-compile`) and verify existing tests continue to pass.
3. **Phase 2C — Verification & Gate**:
   - Dispatch 2 Reviewers independently (code review, clean architecture, API contracts, security checks).
   - Dispatch 2 Challengers (edge cases, race conditions, null safety, error responses).
   - Dispatch 1 Forensic Auditor (integrity verification, genuine logic check).
   - Evaluate Gate verdicts in `GATE_STATUS.md`.

## Milestone 3: Comprehensive Automated Tests & Full Verification
1. **Phase 3A — Test Suite Design & Implementation**:
   - Dispatch Explorers / Test Writers for:
     - `SearchServicesUseCaseTest`
     - `GetServiceDetailUseCaseTest`
     - `RecordRecentlyViewedUseCaseTest`
     - `WishlistUseCaseTest`
     - `CatalogControllerTest` (MockMvc tests)
   - Worker implements comprehensive tests covering all edge cases, access controls (public catalog vs authenticated wishlist), and concurrent atomic updates.
2. **Phase 3B — Verification & Gate**:
   - Reviewers, Challengers, Forensic Auditor verify test suite authenticity and pass rate.
3. **Phase 4 — Full Regression & Final Reporting**:
   - Run complete test suite.
   - Report final completion back to Sentinel with structured findings.
