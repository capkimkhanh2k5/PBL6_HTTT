# Project: Public Catalog, Wishlist & Recently Viewed Module

## Architecture
- **Framework & Runtime**: Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security (Stateless JWT), PostgreSQL.
- **Design Pattern**: Modular Clean Architecture (`com.danasea.backend.modules.service`):
  - `domain`: Core domain models (`Service`, `Category`, `Wishlist`, `RecentlyViewed`, `ServiceStatus`), exceptions (`ServiceNotFoundException`), and repository port interfaces.
  - `application`: Use cases (`SearchServicesUseCase`, `GetServiceDetailUseCase`, `RecordRecentlyViewedUseCase`, `WishlistUseCase`, `GetRecentlyViewedUseCase`) and application DTOs/criteria.
  - `infrastructure`: Persistence entities (`ServiceJpaEntity`, etc.), Spring Data JPA repositories (`JpaServiceRepository`, etc.), repository adapters implementing domain ports, and mappers.
  - `presentation`: REST controllers (`CatalogController`, `WishlistController`, `RecentlyViewedController`), DTO response records, and `@RestControllerAdvice` exception handler.
- **Security Integration**:
  - `GET /api/services` and `GET /api/services/**` are public (`permitAll()`).
  - `GET /api/recently-viewed` is public (`permitAll()`), supporting both logged-in users and guest sessions via header `X-Session-Id`.
  - `/api/wishlists/**` requires authentication (`.anyRequest().authenticated()`).
  - Custom `AuthenticationEntryPoint` returns standardized JSON error (`{"code":"UNAUTHORIZED","message":"Authentication required"}`).
  - `SecurityUtils` extracts authenticated `UUID userId` from SecurityContext (`AuthorizationSubject`).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Domain Repository Ports & Exceptions | Define `ServiceRepositoryPort`, `WishlistRepositoryPort`, `RecentlyViewedRepositoryPort`, `ServiceNotFoundException` | M1 | Survey / Clean Architecture |
| 2 | Atomic View Count Increment Query | JPQL query in `JpaServiceRepository` for atomic DB-level row update without race conditions | M1 | ORIGINAL_REQUEST R1, AC |
| 3 | Dynamic Search Specification | Spring Data JPA Specification filtering by category, keyword, lat/lng/radius, price range, and strictly `PUBLISHED` | M1 | ORIGINAL_REQUEST R1, AC |
| 4 | Wishlist & RecentlyViewed Queries | Custom queries for existence checks, deletion, and finding by userId or sessionId | M1 | ORIGINAL_REQUEST R2, R3 |
| 5 | Persistence Adapters & Mappers | Implement repository ports via JPA adapters and Spring mappers | M1 | Architecture Survey |
| 6 | Security Configuration & Context Utils | Update `SecurityConfig` for public routes, 401 JSON entry point, and `SecurityUtils.getCurrentUserId()` | M1 | ORIGINAL_REQUEST R1, R2, R3 |
| 7 | SearchServicesUseCase | Application use case for search & filter services, ensuring non-PUBLISHED services are never returned | M2 | ORIGINAL_REQUEST R1, AC |
| 8 | GetServiceDetailUseCase | Application use case for service details, invoking atomic view increment, triggering recently viewed, and returning 404 for draft | M2 | ORIGINAL_REQUEST R1, AC |
| 9 | RecordRecentlyViewedUseCase | Application use case to upsert recently viewed history by `userId` or `sessionId` without duplicate records | M2 | ORIGINAL_REQUEST R1, R3, AC |
| 10 | WishlistUseCase | Application use case for adding, removing, and retrieving wishlists with idempotent behavior | M2 | ORIGINAL_REQUEST R2, AC |
| 11 | CatalogController | REST API: `GET /api/services` and `GET /api/services/{id}` | M2 | ORIGINAL_REQUEST R1 |
| 12 | WishlistController | REST API: `POST /api/wishlists/{serviceId}`, `DELETE /api/wishlists/{serviceId}`, `GET /api/wishlists` | M2 | ORIGINAL_REQUEST R2 |
| 13 | RecentlyViewedController | REST API: `GET /api/recently-viewed` with dual user/guest support | M2 | ORIGINAL_REQUEST R3 |
| 14 | CatalogExceptionHandler | Handle `ServiceNotFoundException` returning 404 `SERVICE_NOT_FOUND` | M2 | Clean Architecture / API spec |
| 15 | Spring Bean Configuration | Wire use cases into Spring application context (`ServiceBeans` or `ApplicationBeans`) | M2 | Clean Architecture Rule 7 |
| 16 | SearchServicesUseCaseTest | Unit tests for category, keyword, price range, radius, and status filtering | M3 | ORIGINAL_REQUEST R4 |
| 17 | GetServiceDetailUseCaseTest | Unit tests for detail retrieval, atomic view count increment, 404 for draft/missing, and recently viewed hook | M3 | ORIGINAL_REQUEST R4 |
| 18 | RecordRecentlyViewedUseCaseTest | Unit tests for user and guest recently viewed upserting, timestamp updates, and duplicate prevention | M3 | ORIGINAL_REQUEST R4 |
| 19 | WishlistUseCaseTest | Unit tests for idempotent additions, idempotent removals, and listing wishlists | M3 | ORIGINAL_REQUEST R4 |
| 20 | CatalogControllerTest | MockMvc tests for public access, authenticated wishlist access, status codes (200, 404, 401, 400), and parameter handling | M3 | ORIGINAL_REQUEST R4 |
| 21 | Full Verification & Regression Pass | Run complete test suite verifying 100% pass rate with zero errors | M3 | Acceptance Criteria |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Domain, Persistence & Security Foundations | Ports, JPA queries (atomic increment, Specification), Adapters, Mappers, SecurityConfig, SecurityUtils | None | PLANNED |
| M2 | Use Cases, Business Logic & REST Controllers | SearchServicesUseCase, GetServiceDetailUseCase, WishlistUseCase, RecordRecentlyViewedUseCase, CatalogController, WishlistController, RecentlyViewedController, ExceptionHandler | M1 | PLANNED |
| M3 | Automated Test Suites & Full Verification | SearchServicesUseCaseTest, GetServiceDetailUseCaseTest, RecordRecentlyViewedUseCaseTest, WishlistUseCaseTest, CatalogControllerTest, Full Suite Verification | M2 | PLANNED |

## Interface Contracts
### Public Catalog Endpoints
- `GET /api/services`
  - Parameters: `categoryId` (UUID, opt), `keyword` (String, opt), `minPrice` (BigDecimal, opt), `maxPrice` (BigDecimal, opt), `lat` (BigDecimal, opt), `lng` (BigDecimal, opt), `radiusKm` (Double, opt), `page` (int, default 0), `size` (int, default 20)
  - Access: Public (`permitAll()`)
  - Response: `200 OK` -> `PageResponse<ServiceSummaryResponse>`
- `GET /api/services/{id}`
  - Parameters: Path `id` (UUID), Header `X-Session-Id` (String, opt)
  - Access: Public (`permitAll()`)
  - Behavior: Atomic increment `view_count`; record recently viewed if user authenticated or `X-Session-Id` present.
  - Response: `200 OK` -> `ServiceDetailResponse`, `404 Not Found` -> `{"code":"SERVICE_NOT_FOUND","message":"..."}`

### Wishlist Endpoints
- `POST /api/wishlists/{serviceId}`
  - Parameters: Path `serviceId` (UUID)
  - Access: Authenticated (Bearer JWT)
  - Behavior: Idempotent add. If already in wishlist, returns 200/201 without duplicate entry.
- `DELETE /api/wishlists/{serviceId}`
  - Parameters: Path `serviceId` (UUID)
  - Access: Authenticated (Bearer JWT)
  - Behavior: Idempotent delete. If not in wishlist, returns 200/204 gracefully.
- `GET /api/wishlists`
  - Access: Authenticated (Bearer JWT)
  - Response: `200 OK` -> `List<WishlistItemResponse>`

### Recently Viewed Endpoint
- `GET /api/recently-viewed`
  - Parameters: Header `X-Session-Id` (String, opt)
  - Access: Public (`permitAll()`)
  - Behavior: Returns history for authenticated user if Bearer token present, else for `X-Session-Id`.
  - Response: `200 OK` -> `List<RecentlyViewedResponse>`

## Code Layout
- Domain: `backend/src/main/java/com/danasea/backend/modules/service/domain/`
  - `exceptions/ServiceNotFoundException.java`
  - `ports/ServiceRepositoryPort.java`, `WishlistRepositoryPort.java`, `RecentlyViewedRepositoryPort.java`
- Application: `backend/src/main/java/com/danasea/backend/modules/service/application/`
  - `usecase/SearchServicesUseCase.java`, `GetServiceDetailUseCase.java`, `RecordRecentlyViewedUseCase.java`, `WishlistUseCase.java`, `GetRecentlyViewedUseCase.java`
  - `dto/SearchServicesCriteria.java`, `ServiceSummaryResult.java`, `ServiceDetailResult.java`, `WishlistItemResult.java`, `RecentlyViewedResult.java`
- Infrastructure: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/`
  - `persistence/repositories/JpaServiceRepository.java` (updated with atomic increment & Specification)
  - `persistence/repositories/JpaWishlistRepository.java`, `JpaRecentlyViewedRepository.java`
  - `persistence/adapters/ServiceRepositoryAdapter.java`, `WishlistRepositoryAdapter.java`, `RecentlyViewedRepositoryAdapter.java`
  - `mapper/ServiceMapper.java`, `WishlistMapper.java`, `RecentlyViewedMapper.java`
- Presentation: `backend/src/main/java/com/danasea/backend/modules/service/presentation/`
  - `CatalogController.java`, `WishlistController.java`, `RecentlyViewedController.java`
  - `CatalogExceptionHandler.java`
  - `dto/ServiceSummaryResponse.java`, `ServiceDetailResponse.java`, `WishlistItemResponse.java`, `RecentlyViewedResponse.java`
- Security & Config:
  - `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`
  - `backend/src/main/java/com/danasea/backend/security/infrastructure/SecurityUtils.java`
  - `backend/src/main/java/com/danasea/backend/security/authentication/infrastructure/security/JwtAuthenticationFilter.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/config/ServiceBeans.java`
- Tests: `backend/src/test/java/com/danasea/backend/modules/service/`
  - `application/usecase/SearchServicesUseCaseTest.java`
  - `application/usecase/GetServiceDetailUseCaseTest.java`
  - `application/usecase/RecordRecentlyViewedUseCaseTest.java`
  - `application/usecase/WishlistUseCaseTest.java`
  - `presentation/CatalogControllerTest.java`
