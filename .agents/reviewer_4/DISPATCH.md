# Dispatch — reviewer_4

**Identity**: `reviewer_4` (teamwork_preview_reviewer)
**Parent**: `orchestrator_2`
**Working Directory**: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/reviewer_4`
**Task**: Security, Constraints, Exception Handling & Interface Conformance Review for Categories Module.

## Context & Files to Read First:
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/ORIGINAL_REQUEST.md` (MANDATORY: read this first verbatim)
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/PROJECT.md`
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/worker_2/handoff.md`
- Backend source code in `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/backend`

## Review Focus:
1. Security configuration: verify `GET /api/categories` and `GET /api/categories/**` are `permitAll()`, while `/api/admin/**` endpoints are strictly protected by `ROLE_ADMIN` (via URL matching and/or `@PreAuthorize`).
2. Domain exceptions & REST responses: verify `CategoryNotFoundException` (404), `SlugAlreadyExistsException` (409 or 400), `CategoryHasActiveServicesException` (400 or 409), `CategoryHierarchyLoopException` (400) are cleanly handled by `@RestControllerAdvice` returning standard `ErrorResponse`.
3. Database constraint verification: table name `categories`, unique constraint on `slug`, not-null constraints.
4. Active services check & Deactivation logic: verify soft-delete (`isActive = false`) and query `existsByCategoryIdAndStatus` with `ServiceStatus.PUBLISHED`.
5. Verification: Execute full test suite:
   ```bash
   cd /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/backend
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   ./mvnw test
   ```
6. Write your complete review report to `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/reviewer_4/handoff.md` with verdict APPROVE or REQUEST_CHANGES, and send message to parent.
