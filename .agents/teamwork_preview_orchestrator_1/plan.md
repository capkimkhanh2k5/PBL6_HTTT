# Plan: Public Catalog, Wishlist, Recently Viewed Module Implementation

## Objective
Implement the Public Catalog, Wishlist, and Recently Viewed module along with comprehensive automated testing as specified in ORIGINAL_REQUEST.md.

## Phases
1. **Phase 0: Survey & Architecture Discovery**
   - Dispatch 3 parallel Explorers:
     - Explorer 1: Project structure, build tools (Maven/Gradle), existing domain entities, service models, repositories.
     - Explorer 2: Existing controller/security setup, authentication/session management for guest/user, database schema/migrations.
     - Explorer 3: Existing testing framework, MockMvc configuration, test fixtures, current test suite status.
   - Aggregate findings into PROJECT.md.

2. **Phase 1: Architecture, Decomposition & Interface Contracts**
   - Create PROJECT.md defining modules, feature inventory, interface contracts, and milestones.
   - Initialize E2E Testing Track and Implementation Track.

3. **Phase 2: Milestone Execution**
   - Milestone 1: Domain Entities & Repositories (Services, Wishlist, RecentlyViewed, status/view_count atomic updates).
   - Milestone 2: Use Cases & Business Logic (SearchServicesUseCase, GetServiceDetailUseCase, WishlistUseCase, RecordRecentlyViewedUseCase).
   - Milestone 3: Controllers & REST APIs (Public Catalog, Wishlist, Recently Viewed, Auth/Guest handling).
   - Parallel Dual Track: E2E Test Suite & Test Infra.

4. **Phase 3: Integration & Final E2E Test Pass**
   - Pass 100% of E2E tests across Tiers 1-4.
   - Run adversarial coverage hardening (Tier 5).

5. **Phase 4: Final Audits & Completion Report**
   - Forensic audit verification.
   - Report final completion and summary to Sentinel.
