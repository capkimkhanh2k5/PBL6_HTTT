# Reschedule and weather fallback contract

Vendor proposals are invitations, not holds. The customer reserves the new slot only when accepting a valid proposal or an active weather-driven change. Confirmation keeps the original service/vendor, option, price and quantity; cross-vendor changes and price differences require a separate new booking.

The lock order is MasterOrder → SubOrder → slots sorted by UUID. Both reschedule and weather cancellation use this order and verify the current slot after acquiring locks. Redis plans capacity against committed DB counts and existing holds; confirmation uses that exact plan, commits the target capacity, releases the original allocation and writes history in one DB transaction. Temporary Redis holds remain until transaction completion and expire after two minutes if the process stops.

Private packages require completely empty units with capacity at least the frozen package limit and the booked participants. Shared groups stay together unless the original booking explicitly permitted splitting. New bookings freeze `maxPaxPerPackage` and `allowSplit`; legacy bookings derive a conservative package limit from current policy, original private allocation and participants per package. Existing allocation history proves consent for already split shared bookings.

`GET /api/sub-orders/{id}/reschedule-options` is advisory and includes active Redis holds. `POST /api/vendor/sub-orders/{id}/reschedule-proposals` accepts `proposedSlotId`, `reason`, `reasonType` (`OPERATIONAL`/`WEATHER`), and optional `expiresAt`. The default expiry never precedes now and cannot exceed either departure or 24 hours. `POST /api/sub-orders/{id}/reschedule` requires an owner customer, `Idempotency-Key`, `targetSlotId`, optional `proposalId`, and `expectedVersion`. State/inventory/version conflicts return HTTP 409 with stable `RESCHEDULE_CONFLICT`; another user's order returns 403.

Replay validates ownership before reading success history and fingerprints all confirmation fields. A successful change updates booking/sub-order slots and times, increments the version, rotates the QR secret, removes unused check-in tokens, and writes an audit record. The domain/JPA mapper preserves the reschedule snapshot during later order updates.

Proposal/history delivery markers form a durable notification queue. AFTER_COMMIT handlers send through REQUIRES_NEW transactions; scheduled retries use idempotent notification keys, so a delivery failure never undoes a committed booking change. Existing trip reminders query the new slot.

Weather source timestamps are never refreshed merely by reading another cache. Weather/marine forecasts are fresh for 30 minutes and may fall back for display up to two hours from the original fetch, including Redis data restored after a process restart. Beyond that age, data is unavailable. Stale/missing observations cannot certify safety or clear active weather warnings; the advance API exposes `safetyStatus=UNKNOWN`, `isSafe=false`, source age and coverage metadata. Missing marine values are not represented as actual marine estimates in the public weather response.

Manual/automatic weather cancellation rechecks the locked sub-order's current slot, cancels only confirmed paid bookings without check-in, releases committed capacity once and creates a full-refund request in PENDING. Provider confirmation owns the final refund status. Dismissing/cancelling an alert changes its resolution status while preserving the meteorological safety verdict.

Flyway uses V36 for this feature and V35 for waiver acceptance. No existing shared development database was modified by this fix; validation runs use isolated test databases.
