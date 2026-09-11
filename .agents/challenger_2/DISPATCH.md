# Task Assignment for challenger_2

## 2026-09-10T03:57:30Z

**Role**: Empirical Adversarial Verifier — Business Logic & State Transition Tester
**Working Directory**: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/challenger_2
**Original Request**: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/ORIGINAL_REQUEST.md
**Project Plan**: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/PROJECT.md
**Worker Handoff**: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/.agents/worker_2/handoff.md

## Mission
Perform empirical adversarial testing on the Categories module business logic:
1. Deactivation constraints: Verify deactivation is strictly blocked when category has active services (`ServiceStatus.PUBLISHED`). Verify deactivation succeeds when services are DRAFT, REJECTED, or no services exist.
2. Slug uniqueness edge cases: Case sensitivity, updates keeping existing slug vs updating to already taken slug.
3. Inactive category filtering: Verify inactive categories are completely excluded from public tree response, but present in admin response.
4. Run tests:
   ```bash
   cd /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/backend
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   ./mvnw test -Dtest="*Category*UseCaseTest"
   ```
   (Use `BypassSandbox: true` when running commands).
5. Deliver verdict in `handoff.md`: APPROVE or REQUEST_CHANGES.
