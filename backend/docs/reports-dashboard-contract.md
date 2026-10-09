# Reports and dashboard contract

Scope: `implement_admin_reports_dashboard`, API inventory 123 method/path combinations (41 controllers).

## Endpoints

| Role | Method / path | Filters |
|---|---|---|
| Admin | GET /api/admin/dashboard | optional from, to, vendorId |
| Admin | GET /api/admin/reports/revenue | from, to, groupBy, optional vendorId |
| Admin | GET /api/admin/reports/bookings | from, to, groupBy, optional vendorId |
| Admin | GET /api/admin/reports/vendors | from, to, optional vendorId |
| Admin | GET /api/admin/reports/export | type, from, to, groupBy, optional vendorId |
| Vendor | GET /api/vendor/dashboard | optional from, to |
| Vendor | GET /api/vendor/reports/revenue | from, to, groupBy |
| Vendor | GET /api/vendor/reports/bookings | from, to, groupBy |
| Vendor | GET /api/vendor/reports/export | type, from, to, groupBy |

Vendor identity always comes from the authenticated user. Admin methods require ADMIN; vendor methods require VENDOR and a vendor profile. Export type is `revenue`, `bookings` or `vendors` (vendor export of vendors includes only that vendor). Output is CSV with UTF-8 BOM, RFC 4180 quoting and formula neutralization for untrusted business names.

`groupBy` is day (default), week (ISO), quarter or year. Dates use Asia/Ho_Chi_Minh. The selected dates include the entire last date using `[from 00:00, day-after-to 00:00)`. Invalid/reversed ranges and ranges over 3660 inclusive days return HTTP 400. Dashboard defaults to today minus 30 days through today. Empty periods are zero filled.

## Financial definitions

All monetary amounts use the internal order currency (VND), not the provider currency amount.

- A payment SUCCESS creates the original cash event at immutable `paid_at`. A payment later marked REFUNDED retains this event. Pending/failed payments contribute nothing.
- GMV is the sum of sub-order gross amounts associated with successful payment events. Cash is the actual captured payment amount, allocated proportionally across **all** sub-orders in the master order before vendor filtering. The largest remainder method preserves cents and exact platform/vendor totals.
- Discount is gross minus allocated cash; it is derived from the captured amount, rather than subtracting unrelated/unpaid master-order discounts.
- Refunds include only PROCESSED records at `processed_at`, including requests created in previous periods. Ownership is resolved from the refund's actual sub-order, regardless of its creation date.
- Commission on payment uses the sub-order commission rate. Refund adjustments reverse the difference between rounded commission before and after the cumulative processed refund. Full refund reverses the full original commission; partial refund uses the remaining gross amount, matching the settlement rate policy.
- `netVendorPayout = collectedCash - refunds - platformCommission`. This is a financial balance attributable to the period, **not confirmation of a bank payout**. It does not replace settlement eligibility, dispute holds or payout status.
- Refund-only periods can have negative commission adjustments and negative vendor payout. These signed amounts preserve accounting totals across day/week/quarter/year aggregation.
- Dashboard, revenue and vendor performance use the same financial ledger under a read-only repeatable-read transaction.

Example: 1000 gross/cash, 10% commission, processed refund 500 in the same day gives cash 1000, refunds 500, commission 50, payout 450. A full refund next day produces payment-day payout 900 and refund-day adjustment -900; the combined payout is zero.

## Booking definitions

Booking reports are **creation cohorts**: sub-orders created in the selected range, classified by current status. They are not counts of status-change events occurring in that range. Rates use sub-order count, not master-order count or passenger quantity.

Each cancelled sub-order contributes once to the cancellation breakdown. Persisted `cancellation_reason` covers zero-refund customer cancellations and vendor rejection; legacy fallback uses the related refund history. Compensation/dispute refunds alone do not establish a cancellation. Missing legacy reasons are exposed by `unknownCancellationCount`, also present in CSV.

## Migration and historical limitations

Apply Flyway `V22__report_financial_events.sql` before running this code against an existing database. It adds payment success time, cancellation reason and supporting indexes. Existing SUCCESS/REFUNDED rows use `updated_at` (or `created_at`) as a **best available estimate**, because the original success time was not persisted. Accurate historical payment dates require reconciliation with provider records; migration cannot reconstruct missing history. Missing legacy cancellation reasons remain unknown rather than fabricated.

## Verification

`ReportFinancialIntegrationTest` uses PostgreSQL 16 and real report queries/usecases with Flyway. Cases cover unpaid orders, failed/pending gateway events, cash/refund periods, cross-midnight boundaries, full/partial refunds, rounded repeated refunds, vendor allocation/isolation, dashboard consistency, cancellation reasons and spreadsheet safety. `PostgreSqlMigrationIntegrationTest` also verifies upgrade from V17 and backfill.

Run `./mvnw clean verify` from backend. Security integration tests verify JWT/RBAC and vendor profile resolution. No test result here establishes a live gateway reconciliation or a production deployment.

Verified 09/10/2026 in this worktree: `./mvnw clean verify` BUILD SUCCESS, 1875 tests, 0 failures/errors, 135 skipped. Report module: 288 tests, 0 failures/errors, 0 skipped, including 14 PostgreSQL financial integration tests in an isolated schema with Hibernate validation. Google Java Format check and `git diff --check` passed. No live application database was migrated.
