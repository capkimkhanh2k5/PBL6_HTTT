# Dispatch to Explorer M2-3 (Exception Handling & Spring Configuration)

## Working Directory
`/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/.agents/teamwork_preview_explorer_m2_3`

## Task Assignment
Milestone 2 Explorer: Investigate Global Exception Handling, Error Response format, and Spring Bean Configuration.
Refer to:
- `ORIGINAL_REQUEST.md`: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/ORIGINAL_REQUEST.md`
- `PROJECT.md`: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/PROJECT.md`
- Milestone 1 Handoff: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/.agents/teamwork_preview_worker_m1/handoff.md`

Investigate and document in your `handoff.md`:
1. Existing exception handlers in `backend` (check `@RestControllerAdvice`, standard error structures across modules).
2. Detailed design and code blueprints for:
   - `CatalogExceptionHandler`: Mapping `ServiceNotFoundException` to HTTP 404 with exact body format (`{"code":"SERVICE_NOT_FOUND","message":"..."}`), handling parameter validation errors (HTTP 400).
   - `ServiceBeans`: Configuration class to wire use cases, repositories, adapters, and domain services in compliance with Clean Architecture.
3. Check how Spring Boot component scanning is structured in `com.danasea.backend`.
4. Concrete recommendations for Worker.
