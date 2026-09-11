# Plan: Milestone 3 & Phase 4 Verification

## Objective
Finalize the Vendor Profile Module by implementing the complete test suite (Unit & MockMvc tests), verifying all tests pass, running multi-agent adversarial reviews and audits, and reporting completion to the Sentinel.

## Step Breakdown

### Step 1: Initialization & Heartbeat Setup
- [x] Record DISPATCH.md
- [x] Create BRIEFING.md
- [ ] Create plan.md and progress.md
- [ ] Schedule recurring heartbeat cron (every 10 minutes)

### Step 2: Milestone 3 - Test Suite Implementation & Verification
- Dispatch `teamwork_preview_worker` (`worker_m3`) to write and run:
  1. `RegisterVendorProfileUseCaseTest`:
     - Successful registration (status=PENDING, role update to VENDOR via AccountInternalApi)
     - Duplicate registration (`VendorAlreadyExistsException`)
     - Locked user prevention (`UserLockedException` when user.isLocked() is true)
  2. `UpdateVendorProfileUseCaseTest`:
     - Valid profile updates (businessName, taxCode, address, bank account fields)
     - Mass assignment prevention (ignoring verificationStatus, ratingAvg, badgeTier)
     - Cross-vendor manipulation prevention (fetching vendor via userId from context, not path param)
  3. `UploadVendorDocumentUseCaseTest`:
     - Successful upload (status PENDING, reviewed_by and reviewed_at null)
     - Invalid doc_type not in enum (`InvalidDocTypeException` / 400)
     - Upload before vendor registration (`VendorNotFoundException` / 404)
  4. `VendorProfileControllerTest` (MockMvc):
     - GET `/api/vendor/profile`: 404 for CUSTOMER without a vendor profile
     - 403 Forbidden for ADMIN attempting to access vendor routes
     - POST `/api/vendor/profile`: 201 Created on valid customer registration
     - PATCH `/api/vendor/profile`: 200 OK with updated fields
     - POST `/api/vendor/documents`: 201 Created with valid file and doc_type
     - GET `/api/vendor/documents`: 200 OK with list of documents
- Worker verification command:
  `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=RegisterVendorProfileUseCaseTest,UpdateVendorProfileUseCaseTest,UploadVendorDocumentUseCaseTest,VendorProfileControllerTest`
- Wait for Worker 3 handoff report.

### Step 3: Phase 4 - Multi-Agent Verification & Audit
- Dispatch 2 `teamwork_preview_reviewer` subagents to review code correctness, clean architecture adherence, and test completeness.
- Dispatch 2 `teamwork_preview_challenger` subagents to perform adversarial testing, edge case validation, and boundary checking.
- Dispatch 1 `teamwork_preview_auditor` to conduct forensic integrity audit (detect cheating, hardcoding, facade patterns).

### Step 4: Gate Evaluation
- Record verdicts in `GATE_STATUS.md`.
- Ensure all reviewers APPROVE, all challengers confirm correctness, and auditor reports CLEAN.

### Step 5: Reporting & Victory Claim
- Write final `handoff.md`.
- Send victory message to Sentinel (`fd55d950-c6c4-4b47-a58d-582ea8918d1b`) via `send_message`.
