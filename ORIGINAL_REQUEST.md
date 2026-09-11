# Original User Request

## 2026-09-10T03:40:25Z

# Teamwork Project Prompt — Draft

> Status: Launched
> Goal: Craft prompt → get user approval → delegate to teamwork_preview
> Requested team: [none — full standard team will be used]

Implement the Public Catalog module (Customer browsing/searching services), Wishlist, and Recently Viewed features, including REST APIs and comprehensive unit/integration tests.

Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module
Integrity mode: development

## Requirements

### R1. Implement Public Catalog APIs
- `GET /api/services`: Search and filter services by category, keyword, location (lat/lng/radius), and price range. Must only return services with status `PUBLISHED`.
- `GET /api/services/{id}`: Retrieve public service details. Must increment the `view_count` and record the view in the recently viewed history (upsert by `user_id` or `session_id`). Must return 404 for non-PUBLISHED services.

### R2. Implement Wishlist APIs
- `POST /api/wishlists/{serviceId}`: Add a service to the user's wishlist.
- `DELETE /api/wishlists/{serviceId}`: Remove a service from the wishlist.
- `GET /api/wishlists`: Retrieve the user's wishlist.

### R3. Implement Recently Viewed API
- `GET /api/recently-viewed`: Retrieve the user's or guest's recently viewed history.

### R4. Implement Automated Tests
- Create `SearchServicesUseCaseTest`, `GetServiceDetailUseCaseTest`, `RecordRecentlyViewedUseCaseTest`, `WishlistUseCaseTest`, and `CatalogControllerTest` covering all specified edge cases (e.g., race conditions on view_count, idempotent wishlist adds, 404 for draft services).

## Acceptance Criteria

### API Functionality
- [ ] `GET /api/services` correctly filters by all parameters and never exposes non-PUBLISHED services.
- [ ] `GET /api/services/{id}` handles concurrent view count increments safely (e.g., atomic updates).
- [ ] Recently viewed history handles both logged-in users (`user_id`) and guests (`session_id`), updating timestamps without duplicating records.
- [ ] Wishlist endpoints handle duplicate additions and non-existent service removals gracefully.

### Test Coverage
- [ ] All specified use case tests and controller tests pass programmatically.
- [ ] MockMvc tests verify correct access control (Catalog endpoints require login, but `GET /api/services` and `GET /api/services/{id}` are fully public).
