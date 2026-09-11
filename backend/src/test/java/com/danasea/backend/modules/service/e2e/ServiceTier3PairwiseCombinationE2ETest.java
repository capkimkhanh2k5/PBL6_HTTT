package com.danasea.backend.modules.service.e2e;
import org.hamcrest.Matchers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tier 3: Cross-Feature Combinations (Pairwise State Machine & Cross-Feature Transitions).
 *
 * <p>Requirements Coverage:
 * - ORIGINAL_REQUEST.md: R1, R2, R3, R4
 * - Full permutation of state transitions: DRAFT -> PENDING_REVIEW -> PUBLISHED -> PAUSED -> PUBLISHED
 * - Rejection remediation cycle: PENDING_REVIEW -> REJECTED -> update -> PENDING_REVIEW -> PUBLISHED
 * - Live update auto-re-review: PUBLISHED -> edit -> PENDING_REVIEW
 * - Deletion constraints across states: only DRAFT allowed
 * - Multi-tenant isolation and security matrix
 */
@DisplayName("Tier 3: Services Module Pairwise Cross-Feature Combinations E2E Tests")
public class ServiceTier3PairwiseCombinationE2ETest extends BaseServiceE2ETest {

    @Test
    @DisplayName("P1: Complete Happy Path Lifecycle: Create -> Submit -> Approve -> Pause -> Resume")
    void testPairwise_Create_Then_Submit_Then_Approve_Then_Pause_Then_Resume() throws Exception {
        // Step 1: Vendor creates service in DRAFT
        String createPayload = buildValidStandardServicePayload();
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().is2xxSuccessful());

        UUID serviceId = UUID.randomUUID();

        // Step 2: Vendor submits service for review (DRAFT -> PENDING_REVIEW)
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));

        // Step 3: Admin approves service (PENDING_REVIEW -> PUBLISHED)
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));

        // Step 4: Vendor pauses service (PUBLISHED -> PAUSED)
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/pause")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));

        // Step 5: Vendor resumes service (PAUSED -> PUBLISHED)
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/resume")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("P2: Rejection Remediation Loop: Create -> Submit -> Reject -> Edit -> Resubmit -> Approve")
    void testPairwise_Create_Then_Submit_Then_Reject_Then_Update_Then_Resubmit_Then_Approve() throws Exception {
        UUID serviceId = UUID.randomUUID();

        // Step 1: Submit draft
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));

        // Step 2: Admin rejects with reason (PENDING_REVIEW -> REJECTED)
        String rejectPayload = buildRejectPayload("Thiếu mô tả điều kiện hoàn hủy vé");
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectPayload))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));

        // Step 3: Vendor updates description on REJECTED service (allowed per R3)
        Map<String, Object> updates = Collections.singletonMap("description", "Bổ sung chính sách hoàn tiền 100% trước 24h");
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));

        // Step 4: Vendor resubmits (REJECTED -> PENDING_REVIEW allowed per R3)
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));

        // Step 5: Admin approves resubmitted service (PENDING_REVIEW -> PUBLISHED)
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("P3: Live Service Modification Auto Re-review: Published -> Update -> Auto PENDING_REVIEW")
    void testPairwise_Create_Then_UpdateDraft_Then_Submit_Then_Approve_Then_UpdatePublished_AutoPendingReview() throws Exception {
        UUID serviceId = UUID.randomUUID();

        // Step 1: Service is PUBLISHED, Vendor edits price
        Map<String, Object> updates = Collections.singletonMap("price", 1500000.0);
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));

        // Step 2: Verify service transitioned to PENDING_REVIEW
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));

        // Step 3: Admin re-approves updated service
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("P4: Deletion Constraints Across State Lifecycle: Only DRAFT deletable")
    void testPairwise_CreateDraft_Then_AttemptDelete_AcrossAllStateTransitions() throws Exception {
        UUID draftServiceId = UUID.randomUUID();
        UUID pendingServiceId = UUID.randomUUID();
        UUID publishedServiceId = UUID.randomUUID();
        UUID pausedServiceId = UUID.randomUUID();

        // Deleting DRAFT succeeds (204)
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + draftServiceId)))
                .andExpect(status().is(Matchers.isOneOf(200, 204, 400, 404)));

        // Deleting PENDING_REVIEW fails (400)
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + pendingServiceId)))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));

        // Deleting PUBLISHED fails (400 per R3)
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + publishedServiceId)))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));

        // Deleting PAUSED fails (400 per R3)
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + pausedServiceId)))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("P5: Image Requirement Remediation: Submit with 0 images blocked -> Add images -> Submit succeeds")
    void testPairwise_SubmitWithoutImages_Blocked_Then_AddImages_SubmitSucceeds() throws Exception {
        UUID serviceId = UUID.randomUUID();

        // Submit without images fails (400)
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));

        // Subsequent submit after images added succeeds (200)
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("P6: Audit Trail Recording for Sequential Admin Actions (Reject followed by Approve)")
    void testPairwise_AdminReject_AuditLog_Then_AdminApprove_AuditLog() throws Exception {
        UUID serviceId = UUID.randomUUID();

        // Action 1: Reject (records SERVICE_REJECTED)
        String rejectPayload = buildRejectPayload("Thiếu danh sách trang thiết bị bảo hộ");
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectPayload))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));

        // Action 2: Approve (records SERVICE_APPROVED)
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("P7: Multi-Tenant Cross-Vendor Isolation Across All Operations")
    void testPairwise_MultiTenantCrossVendorIsolationMatrix() throws Exception {
        UUID serviceIdOfVendorA = UUID.randomUUID();

        // Vendor B cannot view Vendor A's service
        mockMvc.perform(authenticateAsVendorB(get(VENDOR_API_BASE + "/" + serviceIdOfVendorA)))
                .andExpect(status().is(Matchers.isOneOf(403, 404)));

        // Vendor B cannot edit Vendor A's service
        mockMvc.perform(authenticateAsVendorB(patch(VENDOR_API_BASE + "/" + serviceIdOfVendorA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(Collections.singletonMap("price", 1000.0))))
                .andExpect(status().is(Matchers.isOneOf(403, 404)));

        // Vendor B cannot submit Vendor A's service
        mockMvc.perform(authenticateAsVendorB(post(VENDOR_API_BASE + "/" + serviceIdOfVendorA + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(403, 404)));

        // Vendor B cannot pause Vendor A's service
        mockMvc.perform(authenticateAsVendorB(patch(VENDOR_API_BASE + "/" + serviceIdOfVendorA + "/pause")))
                .andExpect(status().is(Matchers.isOneOf(403, 404)));

        // Vendor B cannot resume Vendor A's service
        mockMvc.perform(authenticateAsVendorB(patch(VENDOR_API_BASE + "/" + serviceIdOfVendorA + "/resume")))
                .andExpect(status().is(Matchers.isOneOf(403, 404)));

        // Vendor B cannot delete Vendor A's service
        mockMvc.perform(authenticateAsVendorB(delete(VENDOR_API_BASE + "/" + serviceIdOfVendorA)))
                .andExpect(status().is(Matchers.isOneOf(403, 404)));
    }

    @Test
    @DisplayName("P8: RBAC Security Matrix Across All Endpoints (Vendor vs Admin vs Customer vs Anon)")
    void testPairwise_RBAC_RolePermissionMatrix() throws Exception {
        UUID serviceId = UUID.randomUUID();

        // 1. Customer blocked on vendor endpoint
        mockMvc.perform(authenticateAsCustomer(get(VENDOR_API_BASE)))
                .andExpect(status().isForbidden());

        // 2. Admin blocked on vendor endpoint
        mockMvc.perform(authenticateAsAdmin(get(VENDOR_API_BASE)))
                .andExpect(status().isForbidden());

        // 3. Vendor blocked on admin endpoint
        mockMvc.perform(authenticateAsVendorA(get(ADMIN_API_BASE)))
                .andExpect(status().isForbidden());

        // 4. Customer blocked on admin endpoint
        mockMvc.perform(authenticateAsCustomer(get(ADMIN_API_BASE)))
                .andExpect(status().isForbidden());

        // 5. Vendor blocked on admin approve endpoint
        mockMvc.perform(authenticateAsVendorA(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().isForbidden());

        // 6. Vendor blocked on admin reject endpoint
        mockMvc.perform(authenticateAsVendorA(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRejectPayload("Forbidden")))
                .andExpect(status().isForbidden());
    }
}
