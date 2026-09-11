# Dispatch — challenger_4

**Identity**: `challenger_4` (teamwork_preview_challenger)
**Parent**: `orchestrator_2`
**Working Directory**: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/challenger_4`
**Task**: Adversarial Stress Testing on Deactivation Rules, Active Services Blocking, and Slug Uniqueness.

## Context & Files to Read First:
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/ORIGINAL_REQUEST.md` (MANDATORY: read this first verbatim)
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/PROJECT.md`
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/worker_2/handoff.md`
- Backend source code in `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/backend`

## Adversarial Focus:
1. Deactivation Blocking with Service Status:
   - When services have status `PUBLISHED`: MUST throw `CategoryHasActiveServicesException`.
   - When services have status `DRAFT`, `PENDING_REVIEW`, `REJECTED`, or `SUSPENDED`: should deactivation be allowed? (Verify how `ServiceStatus` is checked).
   - When category has 0 services: deactivation succeeds, `isActive` becomes `false`.
   - When category does not exist: throws `CategoryNotFoundException`.
2. Slug Uniqueness & Edge Cases:
   - Duplicate slug on create -> throws `SlugAlreadyExistsException`.
   - Duplicate slug on update to an existing different category -> throws `SlugAlreadyExistsException`.
   - Update category without changing its slug -> should succeed (not self-conflict).
3. Verification: Execute tests:
   ```bash
   cd /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/backend
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   ./mvnw test -Dtest="*Category*UseCaseTest"
   ```
4. Write your findings to `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/challenger_4/handoff.md` with verdict APPROVE or REQUEST_CHANGES, and send message to parent.

## 2026-09-10T04:10:07Z
User Request:
You are challenger_4 (teamwork_preview_challenger).
Your working directory is: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/challenger_4
Please read your instructions and context at /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/challenger_4/DISPATCH.md and /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/ORIGINAL_REQUEST.md.
Initialize your BRIEFING.md and progress.md in your working directory.
Perform adversarial stress testing on category deactivation blocking (CategoryHasActiveServicesException on PUBLISHED services), slug collisions, and active vs inactive public/admin separation. Run tests via Maven.
Write your report to /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/challenger_4/handoff.md with verdict APPROVE or REQUEST_CHANGES.
Send message back to parent when done.

