# Integrating PR #55 (chat) and PR #52 (AI)

Main already contains V1-V24 and V27. The unmerged feature migrations are assigned V28-V29 (chat) and V30-V34 (AI), preserving all existing main migration files and SQL behavior.

Merge PR #55 before PR #52, then start/migrate the combined backend. If the backend is migrated after each merge, this order keeps migration versions increasing and avoids enabling Flyway out-of-order execution. No changes were made to persistent development databases or Flyway history.

An isolated PostgreSQL upgrade test starts from main's V27 schema, applies the feature migrations, validates them and checks an existing notification and its read/idempotency metadata remain intact. Chat's dedicated V28-to-V29 test preserves messages while merging duplicate conversations and backfilling sequence values.

For a development database that previously applied the unmerged V21-V25 AI or V25-V26 chat scripts, inspect the exact descriptions/checksums and plan the history transition before changing branches. Do not run clean or repair against a persistent database as part of this conflict fix.

## Scoped validation on 2026-10-10

- PR #52: 472 tests reported, 471 passed and one local AI worker smoke test skipped because `ai.local.smoke` was unset; no failures/errors. Runtime inventory matches all 194 documented endpoints.
- PR #55: 328 tests passed with no skips/failures/errors. Runtime inventory matches all 169 documented endpoints.
- Combined merge: 38 migration, schema validation, endpoint inventory, localization, chat/dispute concurrency and P2 regression tests passed with no skips/failures/errors. Runtime inventory matches all 205 documented endpoints.
- PostgreSQL 16 and Redis integration tests use isolated Testcontainers. Migration upgrade checks preserve notification metadata, chat messages and booking inventory invariants.
