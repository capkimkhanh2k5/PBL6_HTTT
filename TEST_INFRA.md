# E2E Test Infrastructure Specification: Services Module

## 1. Test Philosophy

### 1.1 Opaque-Box & Requirement-Driven Approach
The Services Module End-to-End (E2E) Test Suite adopts a strict **Opaque-Box** philosophy:
- **No Implementation Coupling**: Tests treat the backend service as an opaque black box. Test code never references internal entities, repository implementations, or private helper classes.
- **HTTP Client Perspective**: All test assertions evaluate observable HTTP behaviors:
  - REST endpoints (`/api/vendor/services/**` and `/api/admin/services/**`)
  - HTTP request verbs (`GET`, `POST`, `PATCH`, `DELETE`)
  - HTTP headers (`Authorization`, `Content-Type: application/json`)
  - Standard JSON request and response payloads
  - Standard HTTP status codes (`200 OK`, `201 Created`, `204 No Content`, `400 Bad Request`, `403 Forbidden`, `404 Not Found`)
  - Standard error representation (`ErrorResponse(String code, String message)`)
- **Authoritative Expected Output Derivation**:
  - Every expected value is derived directly from requirements in `.agents/ORIGINAL_REQUEST.md` (Sections R1 to R4) and the system architecture in `PROJECT.md`.
  - Business rules, state transitions, validation constraints, and role permissions are codified with deterministic assertions.

### 1.2 Progressive Testability & Stage Gates
- **Milestone Staging**: The E2E suite is authored in the E2E Testing Track ahead of milestone completion.
- **Dynamic Controller Detection**: `BaseServiceE2ETest` inspects classpath availability of `VendorServiceController` and `AdminServiceController`. Before Milestone M4/M5, tests compile cleanly without breaking project builds (`BUILD SUCCESS`) and report staged readiness. When M4 implements presentation controllers, all 135 test cases immediately execute against the live web endpoints without needing test modification.
- **Strict Verification Mode**: Supports `-De2e.strict=true` for gate verification during Milestone M5 to enforce 100% execution pass rate.

---

## 2. Feature Inventory & Test Tier Mapping

| # | Feature | Endpoint & Method | Roles | Precondition | Happy Path (T1) | Boundary/Corner (T2) | Pairwise (T3) | Scenario (T4) | Total Tests |
|---|---------|-------------------|-------|--------------|-----------------|----------------------|---------------|---------------|-------------|
| F1 | Create Service | `POST /api/vendor/services` | VENDOR | Vendor APPROVED, Category Active | 5 | 8 | 4 | 5 | 22 |
| F2 | Get Vendor Services | `GET /api/vendor/services` | VENDOR | Authenticated Vendor | 5 | 5 | 2 | 3 | 15 |
| F3 | Get Service Detail | `GET /api/vendor/services/{id}` | VENDOR | Owner only | 5 | 5 | 2 | 3 | 15 |
| F4 | Update Service | `PATCH /api/vendor/services/{id}` | VENDOR | Owner; DRAFT/REJECTED/PUBLISHED | 5 | 6 | 3 | 4 | 18 |
| F5 | Submit For Review | `POST /api/vendor/services/{id}/submit` | VENDOR | Owner; DRAFT/REJECTED with >=1 image | 5 | 6 | 3 | 4 | 18 |
| F6 | Pause Service | `PATCH /api/vendor/services/{id}/pause` | VENDOR | Owner; PUBLISHED status | 5 | 6 | 2 | 3 | 16 |
| F7 | Resume Service | `PATCH /api/vendor/services/{id}/resume` | VENDOR | Owner; PAUSED status | 5 | 6 | 2 | 3 | 16 |
| F8 | Delete Draft Service | `DELETE /api/vendor/services/{id}` | VENDOR | Owner; DRAFT status only | 5 | 6 | 2 | 3 | 16 |
| F9 | List Pending Services | `GET /api/admin/services?status=PENDING_REVIEW` | ADMIN | ADMIN role | 5 | 5 | 2 | 3 | 15 |
| F10 | Approve Service | `PATCH /api/admin/services/{id}/approve` | ADMIN | ADMIN; PENDING_REVIEW status | 5 | 6 | 3 | 3 | 17 |
| F11 | Reject Service | `PATCH /api/admin/services/{id}/reject` | ADMIN | ADMIN; PENDING_REVIEW with reason | 5 | 8 | 3 | 3 | 19 |
| **Sum** | **Module Coverage** | **All Endpoints** | **VENDOR / ADMIN** | **Full State Machine** | **55** | **67** | **8** | **5** | **135** |

---

## 3. Test Architecture & Runner Details

### 3.1 Directory Layout
```
backend/src/test/java/com/danasea/backend/modules/service/e2e/
├── BaseServiceE2ETest.java                      # Shared test harness, MockMvc setup, auth builders, JSON helpers
├── ServiceTier1FeatureCoverageE2ETest.java      # Tier 1: 55 Feature coverage tests (5 per feature)
├── ServiceTier2BoundaryCornerCaseE2ETest.java   # Tier 2: 67 Boundary, negative, and security tests (>=5 per feature)
├── ServiceTier3PairwiseCombinationE2ETest.java  # Tier 3: 8 Pairwise cross-feature state transition tests
└── ServiceTier4RealWorldScenarioE2ETest.java    # Tier 4: 5 Real-world operational lifecycle scenarios
```

### 3.2 Technology Stack
- **Test Framework**: JUnit Jupiter 5 (`org.junit.jupiter.api.*`)
- **HTTP Mock Engine**: Spring WebMVC Test / MockMvc (`org.springframework.test.web.servlet.MockMvc`)
- **Security Mock**: Spring Security Test (`org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors`)
- **JSON Serialization**: Jackson `ObjectMapper` (2.x / 3.x compatible)
- **Assertion Engine**: MockMvc Result Matchers (`status()`, `jsonPath()`, `content()`, `header()`)

### 3.3 Test Runner Commands

#### 1. Compile Test Suite
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
cd backend
./mvnw test-compile
```

#### 2. Run Individual Tiers
```bash
# Tier 1: Feature Coverage
./mvnw test -Dtest=ServiceTier1FeatureCoverageE2ETest

# Tier 2: Boundary & Corner Cases
./mvnw test -Dtest=ServiceTier2BoundaryCornerCaseE2ETest

# Tier 3: Pairwise Combinations
./mvnw test -Dtest=ServiceTier3PairwiseCombinationE2ETest

# Tier 4: Real-World Scenarios
./mvnw test -Dtest=ServiceTier4RealWorldScenarioE2ETest
```

#### 3. Run Entire E2E Test Suite
```bash
./mvnw test -Dtest="ServiceTier*Test"
```

#### 4. Run in Strict Mode (Milestone M5 Gate Review)
```bash
./mvnw test -Dtest="ServiceTier*Test" -De2e.strict=true
```

---

## 4. Real-World Application Scenarios (Tier 4)

### Scenario 1: Complete Vendor Onboarding & Service Operational Lifecycle
- **Actors**: Verified Vendor A (`vendor_approved_a@danasea.vn`), Platform Admin (`admin_super@danasea.vn`).
- **Flow**:
  1. Vendor A creates a new scuba diving tour (`POST /api/vendor/services`) in `DRAFT` status with dual-language fields, coordinates, capacity, and waiver terms.
  2. Vendor A verifies the draft exists on their dashboard (`GET /api/vendor/services`).
  3. Vendor A attaches photos and submits for official review (`POST /api/vendor/services/{id}/submit`), transitioning to `PENDING_REVIEW`.
  4. Platform Admin queries pending queue (`GET /api/admin/services?status=PENDING_REVIEW`).
  5. Admin approves the service (`PATCH /api/admin/services/{id}/approve`), transitioning it to `PUBLISHED` and writing audit log `SERVICE_APPROVED`.
  6. Due to monsoon warning, Vendor A pauses bookings (`PATCH /api/vendor/services/{id}/pause`), transitioning to `PAUSED`.
  7. When clear skies return, Vendor A resumes the service (`PATCH /api/vendor/services/{id}/resume`), restoring status to `PUBLISHED`.

### Scenario 2: Service Rejection Remediation & Resubmission Lifecycle
- **Actors**: Verified Vendor A, Platform Admin.
- **Flow**:
  1. Vendor A creates and submits a jet ski tour (`DRAFT` -> `PENDING_REVIEW`).
  2. Admin evaluates the submission, finds safety equipment instructions missing, and rejects (`PATCH /api/admin/services/{id}/reject`) with reason: *"Thiếu quy định bắt buộc mặc áo phao và giới hạn độ tuổi người lái môtô nước"*.
  3. System transitions status to `REJECTED` and records audit log `SERVICE_REJECTED` with the reason string.
  4. Vendor A views rejected service detail (`GET /api/vendor/services/{id}`), reviews feedback.
  5. Vendor A updates waiver and description (`PATCH /api/vendor/services/{id}`) with required safety guidelines (status stays `REJECTED` per R3).
  6. Vendor A resubmits the corrected service (`POST /api/vendor/services/{id}/submit`), transitioning `REJECTED` -> `PENDING_REVIEW`.
  7. Admin re-evaluates the revised service and approves (`PATCH /api/admin/services/{id}/approve`), transitioning to `PUBLISHED`.

### Scenario 3: Live Service Modification Auto-Triggers Admin Re-Review
- **Actors**: Verified Vendor A, Platform Admin.
- **Flow**:
  1. Service is actively `PUBLISHED` on DanaSea catalog.
  2. Vendor A modifies price from 800,000 VND to 1,200,000 VND and extends duration (`PATCH /api/vendor/services/{id}`).
  3. Under business rule R3, the platform automatically transitions the service from `PUBLISHED` to `PENDING_REVIEW` to protect consumers from unverified modifications.
  4. While under re-review, bookings are locked and deletion is blocked (`DELETE` returns 400).
  5. Admin reviews the price change justification in the pending list and re-approves (`PATCH /api/admin/services/{id}/approve`), restoring `PUBLISHED` status.

### Scenario 4: Multi-Tenant Vendor Isolation & Malicious Exploit Prevention
- **Actors**: Vendor A (owner), Malicious Vendor B, Customer, Platform Admin.
- **Flow**:
  1. Vendor A creates and submits a luxury yacht cruise.
  2. Rival Vendor B attempts to inspect Vendor A's service (`GET /api/vendor/services/{id}`) -> blocked with HTTP 403 Forbidden.
  3. Vendor B attempts to sabotage Vendor A's price to 1 VND (`PATCH /api/vendor/services/{id}`) -> blocked with HTTP 403 Forbidden.
  4. Vendor B attempts to pause Vendor A's service (`PATCH /api/vendor/services/{id}/pause`) -> blocked with HTTP 403 Forbidden.
  5. Vendor B attempts to delete Vendor A's service (`DELETE /api/vendor/services/{id}`) -> blocked with HTTP 403 Forbidden.
  6. Customer attempts to access `/api/vendor/services` -> blocked with HTTP 403 Forbidden.
  7. Vendor A attempts to self-approve their own service via `/api/admin/services/{id}/approve` -> blocked with HTTP 403 Forbidden.
  8. System guarantees complete tenant separation and principle of least privilege.

### Scenario 5: Weather-Sensitive Water Sports Lifecycle & Deletion State Guards
- **Actors**: Verified Vendor A, Platform Admin.
- **Flow**:
  1. Vendor A attempts to create a parasailing service with `weather_sensitive=true` but omits wind/wave limits -> system rejects with HTTP 400 Bad Request (`WEATHER_REQUIREMENTS_MISSING`).
  2. Vendor A provides valid limits (`minWindKmh=18.0`, `maxWaveM=2.0`) -> service successfully created in `DRAFT`.
  3. Service is submitted and approved (`PUBLISHED`).
  4. Vendor A attempts to delete the live `PUBLISHED` service -> system rejects with HTTP 400 Bad Request (`CANNOT_DELETE_NON_DRAFT_SERVICE`).
  5. Vendor A pauses the service and attempts to delete the `PAUSED` service -> system rejects with HTTP 400 Bad Request.
  6. Vendor A creates a temporary draft service and deletes it -> deletion succeeds with HTTP 204 No Content.

---

## 5. Coverage Thresholds & Quality Gates

| Tier | Target Description | Min Required | Actual Implemented | Gate Criteria |
|------|--------------------|--------------|-------------------|---------------|
| **Tier 1** | Feature Coverage | >=5 per feature (>=55) | **55** | 100% Pass in Milestone M5 |
| **Tier 2** | Boundary & Corner Cases | >=5 per feature (>=55) | **67** | 100% Pass in Milestone M5 |
| **Tier 3** | Pairwise Combinations | Pairwise coverage (>=6) | **8** | 100% Pass in Milestone M5 |
| **Tier 4** | Real-World Scenarios | Complete workflows (>=5) | **5** | 100% Pass in Milestone M5 |
| **Total** | **Full E2E Suite** | **>=121** | **135** | **Zero Failures, Zero Flakiness** |

### Quality Gate Requirements for Milestone M5
1. `./mvnw test-compile` compiles cleanly without warnings on Java 21 LTS.
2. `./mvnw test -Dtest="ServiceTier*Test" -De2e.strict=true` executes 135 tests with **0 Failures**, **0 Errors**, and **0 Skipped**.
3. All audit log events (`SERVICE_APPROVED`, `SERVICE_REJECTED`) persist verified audit trails with correct actor userId and metadata.
4. All role-based access control rules (R4) return HTTP 403 `ACCESS_DENIED` with standard `ErrorResponse`.
