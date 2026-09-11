# Progress — Milestone 1: Domain, Persistence & Security Foundations

Last visited: 2026-09-10T03:57:00Z

## Status
- [x] 1. Read ORIGINAL_REQUEST.md, PROJECT.md, and the 3 survey handoffs.
- [x] 2. Inspect existing entities, repositories, security classes, and related files.
- [x] 3. Implement domain exception and ports (`ServiceNotFoundException`, `ServiceRepositoryPort`, `WishlistRepositoryPort`, `RecentlyViewedRepositoryPort`).
- [x] 4. Update JPA repositories (`JpaServiceRepository`, `JpaWishlistRepository`, `JpaRecentlyViewedRepository`).
- [x] 5. Implement `ServiceSpecifications` (dynamic specifications with PUBLISHED status, categoryId, keyword, price range, lat/lng/radiusKm).
- [x] 6. Implement Mappers (`ServiceMapper`, `WishlistMapper`, `RecentlyViewedMapper`).
- [x] 7. Implement Adapters (`ServiceRepositoryAdapter`, `WishlistRepositoryAdapter`, `RecentlyViewedRepositoryAdapter`).
- [x] 8. Update Security (`SecurityConfig`, `JwtAuthenticationFilter`, `SecurityUtils`).
- [x] 9. Compile and run verification tests (`./mvnw test-compile`, `./mvnw test -Dtest=AuthenticationControllerTest`, plus unit tests).
- [x] 10. Write `handoff.md` and report back to parent via `send_message`.
