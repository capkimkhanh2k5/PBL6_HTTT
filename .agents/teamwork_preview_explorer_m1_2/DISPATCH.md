## 2026-09-10T03:48:13Z
You are Explorer 2 for Milestone 1: Domain, Persistence & Security Foundations.

Your Working Directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/.agents/teamwork_preview_explorer_m1_2
Parent Conversation ID: adb2e576-1356-4e14-adfc-71974a3fd054

MANDATORY FIRST STEPS:
1. Read ORIGINAL_REQUEST.md at:
   /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/ORIGINAL_REQUEST.md
2. Read PROJECT.md at:
   /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/PROJECT.md

Task:
Design the JPA Persistence Queries and Dynamic Specification for Milestone 1:
- Atomic JPQL update in `JpaServiceRepository` to increment `view_count` on PUBLISHED services safely without race conditions.
- JPA Specification (`ServiceSpecification`) combining category, keyword, lat/lng/radius (Haversine / Bounding Box), minPrice, maxPrice, and MANDATORY `status = PUBLISHED`.
- Query methods in `JpaWishlistRepository` (e.g. `existsByUserIdAndServiceId`, `deleteByUserIdAndServiceId`, `findAllByUserIdOrderByCreatedAtDesc`).
- Query methods in `JpaRecentlyViewedRepository` (e.g. `findFirstByUserIdAndServiceId`, `findFirstBySessionIdAndServiceId`, `findByUserIdOrderByViewedAtDesc`, `findBySessionIdOrderByViewedAtDesc`).
Specify exact code snippets, annotations, and parameters.
Write your analysis and recommendation report to:
/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/.agents/teamwork_preview_explorer_m1_2/handoff.md
Send a message to parent when done. DO NOT implement code yourself.
