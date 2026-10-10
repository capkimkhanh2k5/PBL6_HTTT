# Verification — 2026-10-10

Before integration with the waiver branch: `./mvnw -q clean '-Dtest=!BackendApplicationTests' test`: exit 0; 2,323 passed / 136 skipped / 0 failed / 0 errors.

Spring integration tests use real PostgreSQL/Redis and validate Flyway plus Hibernate schema. `BackendApplicationTests` is excluded; existing conditional tests account for the skips. A combined source snapshot of both features passed 78 focused regressions without skips and with no textual merge conflict. New Java formatting and `git diff --check` passed. Provider calls in these regressions are simulated; live sandbox checkout was not retested. No shared dev database was changed.

## Integration before merging to main

The integrated branch (waiver V35 + reschedule V36) passed `./mvnw -q clean '-Dtest=!BackendApplicationTests' test`: exit 0; 2,363 passed / 136 skipped / 0 failures / 0 errors (2,499 tests reported). PostgreSQL/Redis regression tests cover both migrations and features. `BackendApplicationTests` remains excluded; provider requests are simulated.

The first integrated run had one failure in the existing `RbacCacheInvalidationIntegrationTest.usersById_evictsCache_immediatelyOnUserUpdate` cache assertion. An isolated rerun passed all 11 tests and the full clean rerun passed all enabled tests; no production code was changed for this intermittent failure.

Workflow PNG validation: 97 files (43 DF, 46 SD, 4 SM, 4 ARCH), valid PNG checksums, and no broken relative documentation links. The two branches share the same diagrams, so they are included once after integration.
