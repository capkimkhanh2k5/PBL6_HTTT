# TEST_READY: Danasea Feature Package P2 E2E Test Suite

**Author**: `test_writer_e2e_gen2`  
**Date**: 2026-10-10  
**Status**: Focused verification: 93 checks passed, 0 failed, 0 skipped. Full-suite results are recorded below.

---

## 1. Overview & Test Architecture

The E2E test suite for **Danasea Feature Package P2 (R1 - R6)** is located under `backend/src/test/java/com/danasea/backend/e2e/p2/`.

The original 69 feature tests exercise the HTTP contracts with MockMvc. `P2RegressionTest` adds 17 database/event/concurrency checks and HTTP regressions on PostgreSQL 16 and Redis 7. External gateway calls and booking confirmation are mocked in that regression class; inventory allocation has its separate integration suite. The real payment/refund services and notification persistence are exercised. These tests validate backend behavior, not delivery through a live payment or push provider.

P2 fixtures use Flyway and `ddl-auto=validate`; setup/teardown clear only the disposable Testcontainers database, and connection pools are bounded to keep contexts from exhausting PostgreSQL connections.

### Progressive Testability Architecture
Tests employ progressive testability guards (`assumeEndpoint`, `assumeParamSupported`, `assumeFieldPresent`, `assumeClassPresent`, `assumeBean`).
- In standard mode: tests for endpoints currently in progress by worker agents gracefully skip, allowing concurrent CI/CD execution without false alarms.
- In strict verification mode (`-De2e.strict=true`): any missing endpoint or parameter fails the test, so missing features cannot silently pass as skipped tests. Scope strict mode to P2; older service E2E suites use their own progressive guards.

---

## 2. Test Suite Inventory & Coverage by Tier

| Test Class | Scope / Description | Total Tests |
|---|---|---|
| `P2Tier1FeatureCoverageTest` | **Tier 1: Feature Coverage** — Isolated happy-path & core behavior for R1-R6 (>=5 per feature) | **31** |
| `P2Tier2BoundaryAndCornerCaseTest` | **Tier 2: Boundary & Corner Cases** — Out-of-bounds, invalid input, IDOR security, and state guards (>=5 per feature) | **30** |
| `P2Tier3CrossFeatureCombinationTest` | **Tier 3: Cross-Feature Combinations** — Multi-criteria search, discovery-to-storefront, notification lifecycle, payment-to-receipt, storefront-to-detail | **5** |
| `P2Tier4RealWorldScenarioTest` | **Tier 4: Real-World User Scenarios** — Full customer journey, multi-user privacy isolation, public data leakage security audit | **3** |
| `P2RegressionTest` | Holds, shared/private inventory, pagination/count, popularity, events, rollback, concurrent reminders and recipient locale | **17** |
| `PdfReceiptGeneratorTest` | Long Vietnamese receipt, page bounds, 20 discounted items, unbroken names and unsupported glyph fallback | **2** |
| `RefundPersistenceIntegrationTest` | Actual persisted refund completion and notification on PostgreSQL | **1** |
| `PostgreSqlMigrationIntegrationTest` | Upgrade and inventory migrations, including V27 notification idempotency | **2** |
| `BackendApplicationTests` | Context and exact tracking/controller endpoint comparison | **2** |
| **TOTAL** | **Focused verification suite** | **93** |

---

## 3. Breakdown by Feature (R1 - R6)

### Feature R1: Extended Search Criteria & Service Discovery (`GET /api/services`)
- **Tier 1 (6 tests)**:
  - `testR1_SearchServices_ByDate`: Filters services with open slots on specified date.
  - `testR1_SearchServices_ByGuestsCapacity`: Filters services where slot available capacity matches or exceeds requested guest count.
  - `testR1_SearchServices_ByVendorId`: Strictly filters catalog by vendor ID.
  - `testR1_SearchServices_ByMinRating`: Filters services meeting minimum star rating threshold.
  - `testR1_SearchServices_SortBy`: Dynamically sorts services (`price_asc`, `price_desc`, `rating_desc`, `views_desc`).
  - `testR1_SearchServices_BackwardCompatibility`: Preserves backward compatibility with `categoryId`, `keyword`, `minPrice`, `maxPrice`, and pagination.
- **Tier 2 (5 tests)**:
  - `testR1_Boundary_MinRatingOutOfRange_ReturnsBadRequest`: Out-of-range rating (`> 5.0` or `< 0.0`) returns HTTP 400.
  - `testR1_Boundary_GuestsZeroOrNegative_ReturnsBadRequest`: Guest count `<= 0` returns HTTP 400.
  - `testR1_Boundary_InvalidDateFormat_ReturnsBadRequest`: Malformed date string returns HTTP 400.
  - `testR1_Boundary_NonExistentVendor_ReturnsEmptyList`: Non-existent vendor UUID returns empty page (HTTP 200).
  - `testR1_Boundary_NoMatchingSlotCapacity_ReturnsEmptyList`: Unattainable guest capacity returns empty page (HTTP 200).

### Feature R2: Extended Public Service Detail & Strict DTO Separation (`GET /api/services/{id}`)
- **Tier 1 (5 tests)**:
  - `testR2_ServiceDetail_IncludesPublicVendorInfo`: Returns `vendorId`, `businessName`, and `badgeTier`.
  - `testR2_ServiceDetail_IncludesOperationalInfo`: Returns `duration`, `capacity`, `participantConditions`, `refundPolicy`, `cancellationPolicy`, `safetyRules`.
  - `testR2_ServiceDetail_IncludesStructuredSlots`: Returns structured slot list with `slotId`, `startTime`, `endTime`, `capacity`, `availableCapacity`, and `price`.
  - `testR2_ServiceDetail_PreservesLegacyAvailableSlots`: Preserves legacy `availableSlots` array for backward compatibility.
  - `testR2_ServiceDetail_NoSensitiveFinancialLeakage`: Validates zero exposure of bank accounts, bank names, card holders, and tax codes.
- **Tier 2 (5 tests)**:
  - `testR2_Boundary_NonExistentService_ReturnsNotFound`: Non-existent service returns HTTP 404.
  - `testR2_Boundary_MalformedServiceId_ReturnsBadRequest`: Malformed UUID returns HTTP 400.
  - `testR2_Boundary_UnauthenticatedAccessAllowed`: Public unauthenticated client access allowed without JWT.
  - `testR2_Boundary_ZeroCapacitySlot_HandledGracefully`: Fully booked slot shows `availableCapacity = 0` without arithmetic underflow.
  - `testR2_Boundary_NullWaiverContent_HandledGracefully`: Services with null waiver content return HTTP 200 without null pointer exceptions.

### Feature R3: Vendor Public Profile & Storefront (`GET /api/vendors/{id}` & `/services`)
- **Tier 1 (5 tests)**:
  - `testR3_PublicVendorProfile_ReturnsAccurateData`: Returns businessName, address, badgeTier, ratingAvg, ratingCount, activeServicesCount.
  - `testR3_PublicVendorProfile_UnauthenticatedAccessAllowed`: Public unauthenticated access allowed without JWT.
  - `testR3_VendorStorefront_ReturnsPublishedServices`: Returns published services for vendor with pagination.
  - `testR3_VendorStorefront_SupportsSorting`: Supports sorting by price or rating.
  - `testR3_PublicVendor_StrictDataSeparation`: Strictly isolates public profile from sensitive vendor financial data.
- **Tier 2 (5 tests)**:
  - `testR3_Boundary_NonExistentVendor_ReturnsNotFound`: Non-existent vendor returns HTTP 404.
  - `testR3_Boundary_MalformedVendorId_ReturnsBadRequest`: Malformed UUID returns HTTP 400.
  - `testR3_Boundary_VendorWithNoServices_ReturnsZeroCountAndEmptyList`: Vendor with 0 services returns `activeServicesCount = 0` and empty list.
  - `testR3_Boundary_StorefrontPageBeyondTotal_ReturnsEmptyList`: Page index beyond total pages returns empty list.
  - `testR3_Boundary_UnauthenticatedAccessAllowed`: Public unauthenticated storefront access allowed.

### Feature R4: In-App Notification Center (`/api/notifications`)
- **Tier 1 (5 tests)**:
  - `testR4_GetNotifications_IncludesReadStatusFields`: Returns notification items with `isRead` and `readAt`.
  - `testR4_MarkNotificationAsRead_UpdatesStatus`: `PATCH /api/notifications/{id}/read` sets `isRead = true` and assigns timestamp.
  - `testR4_MarkAllNotificationsAsRead_UpdatesAll`: `PATCH /api/notifications/read-all` bulk updates all notifications for user.
  - `testR4_UnreadCount_ReturnsAccurateCount`: `GET /api/notifications/unread-count` returns accurate count.
  - `testR4_MarkRead_DecrementsUnreadCount`: Marking single notification read decrements unread count by 1.
- **Tier 2 (5 tests)**:
  - `testR4_Boundary_UnauthenticatedMarkRead_ReturnsUnauthorized`: Unauthenticated request returns HTTP 401.
  - `testR4_Boundary_UnauthenticatedUnreadCount_ReturnsUnauthorized`: Unauthenticated request returns HTTP 401.
  - `testR4_Boundary_IdorAttack_MarkAnotherUserNotification_ReturnsForbidden`: Attacker cannot mark another user's notification as read (HTTP 403/404).
  - `testR4_Boundary_MarkNonExistentNotification_ReturnsNotFound`: Non-existent notification ID returns HTTP 404.
  - `testR4_Boundary_MarkAllRead_WhenAlreadyAllRead_IsIdempotent`: Calling mark-all-read when unreadCount is 0 returns `updatedCount = 0`.

### Feature R5: Event Listeners & Scheduled Reminders
- **Tier 1 (5 tests)**:
  - `testR5_PaymentSuccessEvent_TriggersCustomerNotification`: Publishing `PaymentSuccessEvent` creates customer in-app notification.
  - `testR5_PaymentSuccessEvent_TriggersVendorNotification`: Publishing `PaymentSuccessEvent` creates vendor in-app notification.
  - `testR5_RefundCompletedEvent_TriggersCustomerNotification`: Publishing `RefundCompletedEvent` creates customer in-app notification.
  - `testR5_TripReminderJob_ExecutesWithIdempotency`: Scheduled trip reminder job executes idempotently without duplicate notifications.
  - `testR5_MobilePushNotificationPort_ExecutesWithoutError`: `PushNotificationPort` / `LoggingPushNotificationAdapter` executes cleanly.
- **Tier 2 (5 tests)**:
  - `testR5_Boundary_TripReminder_PastSlot_NotTriggered`: Past departure slots are excluded from reminder job.
  - `testR5_Boundary_TripReminder_DistantFutureSlot_NotTriggered`: Departures > 24 hours away are excluded from immediate reminders.
  - `testR5_Boundary_PaymentEvent_ForNonExistentOrder_HandlesGracefully`: Phantom order ID does not cause unhandled exceptions.
  - `testR5_Boundary_PushNotification_HandlesSpecialCharacters`: Push notification with emojis, unicode, and Vietnamese text executes smoothly.
  - `testR5_Boundary_RefundEvent_HandlesGracefully`: Event with non-existent refund handled safely.

### Feature R6: Customer Receipt Generation & Retrieval (`GET /api/orders/{id}/receipt`)
- **Tier 1 (5 tests)**:
  - `testR6_GetReceipt_Json_ReturnsStructuredReceipt`: Returns JSON receipt with orderId, orderCode, receiptCode, customer, items, and totals.
  - `testR6_GetReceipt_Pdf_WithQueryParam_ReturnsPdfBinary`: `format=pdf` query parameter returns `application/pdf` binary with `%PDF` header.
  - `testR6_GetReceipt_Pdf_WithAcceptHeader_ReturnsPdfBinary`: `Accept: application/pdf` header returns PDF binary.
  - `testR6_GetReceipt_LineItemsAndTotalsMatchOrder`: Line item details match database orders and sub-orders.
  - `testR6_GetReceipt_AdminAccessAllowed`: Admin user can retrieve receipt for any customer's order.
- **Tier 2 (5 tests)**:
  - `testR6_Boundary_UnauthenticatedReceipt_ReturnsUnauthorized`: Unauthenticated request returns HTTP 401.
  - `testR6_Boundary_IdorAttack_CustomerRequestingOtherCustomerReceipt_ReturnsForbidden`: Customer cannot view another customer's receipt (HTTP 403).
  - `testR6_Boundary_UnpaidOrder_ReturnsBadRequest`: Unpaid order returns HTTP 400 (`OrderNotPaidException`).
  - `testR6_Boundary_CancelledUnpaidOrder_ReturnsBadRequest`: Cancelled unpaid order returns HTTP 400.
  - `testR6_Boundary_NonExistentOrder_ReturnsNotFound`: Non-existent order UUID returns HTTP 404.

---

## 4. How to Run the Tests

### Standard Test Execution (Current CI/CD & Dev Worktrees)
To run the complete P2 E2E test suite:
```bash
./mvnw test -Dtest="com.danasea.backend.e2e.p2.**.*Test"
```

### Individual Tier Execution
```bash
# Tier 1 only
./mvnw test -Dtest=P2Tier1FeatureCoverageTest

# Tier 2 only
./mvnw test -Dtest=P2Tier2BoundaryAndCornerCaseTest

# Tier 3 only
./mvnw test -Dtest=P2Tier3CrossFeatureCombinationTest

# Tier 4 only
./mvnw test -Dtest=P2Tier4RealWorldScenarioTest
```

### Strict Mode Execution (Mandatory for Final Milestone M5 Verification)
```bash
./mvnw test -Dtest="P2Tier*Test,P2RegressionTest,PdfReceiptGeneratorTest,PostgreSqlMigrationIntegrationTest,BackendApplicationTests,RefundPersistenceIntegrationTest" -De2e.strict=true
```

---

## 5. Verification Results

Focused strict command (from `backend`):

```bash
./mvnw test -Dtest="P2Tier*Test,P2RegressionTest,PdfReceiptGeneratorTest,PostgreSqlMigrationIntegrationTest,BackendApplicationTests,RefundPersistenceIntegrationTest" -De2e.strict=true
```

- 93 tests executed and passed; no failures, errors or skipped tests.
- Runtime endpoint inventory and tracking contain the same 158 HTTP method/path pairs.
- PDF preview: `backend/target/test-artifacts/long-receipt.pdf`; all four pages rendered and inspected, with Vietnamese text, item 20, discount and final total preserved.
- Push adapter is a dev preview logger. Its return value is `false`; real device push delivery remains unverified and requires a provider integration.
- V27 does not collide with current V25/V26 files on other branches. If a development database already applied the former notification V25, inspect its Flyway history before combining branches; no persistent development database or Flyway history was altered during this fix.

Full backend verification (same worktree, `./mvnw clean verify`):

```text
Tests run: 2168, Failures: 0, Errors: 0, Skipped: 135
BUILD SUCCESS
```

Maven reported 2,033 executed tests passing; the 135 skipped cases belong to the older service E2E suites and do not count as verified behavior. The P2 focused suite has no skipped cases. Packaging succeeded. `git diff --check` passed. The repository has no configured Java style-check plugin; compilation and tests do not establish a separate Checkstyle result.
