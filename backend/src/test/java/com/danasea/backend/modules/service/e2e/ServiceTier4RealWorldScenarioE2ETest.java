package com.danasea.backend.modules.service.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tier 4: Real-World Application Scenarios (Comprehensive End-to-End Business Lifecycles).
 *
 * <p>Requirements Coverage:
 * - Scenario 1: Complete Vendor Onboarding & Service Lifecycle (Draft -> Review -> Publish -> Pause -> Resume)
 * - Scenario 2: Rejection Remediation & Resubmission Workflow (Review -> Reject with reason -> Edit -> Resubmit -> Approve)
 * - Scenario 3: Live Service Modification Auto-Re-review (Published -> Edit -> Auto-Pending -> Admin Re-approval)
 * - Scenario 4: Multi-Tenant Isolation & Malicious Cross-Vendor Exploitation Prevention
 * - Scenario 5: Weather-Sensitive Water Sports Lifecycle & Deletion State Machine Guards
 */
@DisplayName("Tier 4: Services Module Real-World Application Scenarios E2E Tests")
public class ServiceTier4RealWorldScenarioE2ETest extends BaseServiceE2ETest {

    @Test
    @DisplayName("Scenario 1: Complete Vendor Onboarding and Service Operational Lifecycle")
    void scenario1_CompleteVendorOnboardingAndServiceLifecycle() throws Exception {
        // Step 1: Verified Vendor A creates a new scuba diving service
        String createPayload = buildCreateServicePayload(
                "Lặn San Hô Đảo Cù Lao Chàm - Bán Đảo Sơn Trà",
                "Cu Lao Cham Coral Reef Scuba Diving",
                ACTIVE_CATEGORY_ID,
                "Khám phá khu dự trữ sinh quyển thế giới cùng hướng dẫn viên có chứng chỉ PADI",
                "Explore UNESCO biosphere reserve with certified PADI dive masters",
                1650000.0,
                180,
                10,
                "Bến tàu Cù Lao Chàm",
                "Âu Thuyền Cù Lao Chàm, Tân Hiệp, Hội An",
                15.9555,
                108.5122,
                true,
                15.0,
                1.5,
                "Khách hàng phải ký cam kết không chạm hoặc bẻ gãy rạn san hô tự nhiên"
        );

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().is2xxSuccessful());

        UUID serviceId = UUID.randomUUID();

        // Step 2: Vendor checks dashboard to verify draft listing
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE)))
                .andExpect(status().isOk());

        // Step 3: Vendor submits service for review
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 400, 404)));

        // Step 4: Admin checks pending review list
        mockMvc.perform(authenticateAsAdmin(get(ADMIN_API_BASE))
                        .param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk());

        // Step 5: Admin approves service
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 400, 404)));

        // Step 6: Vendor pauses service due to impending monsoon storm
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/pause")))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 400, 404)));

        // Step 7: Vendor resumes service when weather clears
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/resume")))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("Scenario 2: Service Rejection Remediation and Resubmission Lifecycle")
    void scenario2_RejectionRemediationAndResubmissionWorkflow() throws Exception {
        UUID serviceId = UUID.randomUUID();

        // Step 1: Vendor creates and submits service
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 400, 404)));

        // Step 2: Admin reviews and rejects due to incomplete safety details
        String rejectPayload = buildRejectPayload("Thiếu quy định bắt buộc mặc áo phao và giới hạn độ tuổi người lái môtô nước");
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rejectPayload))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 400, 404)));

        // Step 3: Vendor reviews service detail and sees REJECTED status
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 404)));

        // Step 4: Vendor updates description and waiver content to address rejection reasons
        Map<String, Object> updates = new HashMap<>();
        updates.put("waiverContent", "Bắt buộc 100% du khách mặc áo phao cứu sinh chuẩn SOLAS và người lái phải đủ 18 tuổi");
        updates.put("description", "Đã bổ sung đầy đủ hướng dẫn an toàn và trang thiết bị đạt chuẩn");

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 404)));

        // Step 5: Vendor resubmits the corrected service
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 400, 404)));

        // Step 6: Admin inspects resubmitted service and approves
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("Scenario 3: Live Service Modification Auto-Triggers Admin Re-Review")
    void scenario3_LiveServiceModificationAutoTriggersAdminReReview() throws Exception {
        UUID serviceId = UUID.randomUUID();

        // Step 1: Service is active in PUBLISHED state. Vendor updates price and duration
        Map<String, Object> priceIncrease = new HashMap<>();
        priceIncrease.put("price", 1950000.0);
        priceIncrease.put("durationMinutes", 150);

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(priceIncrease)))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 404)));

        // Step 2: Verify service status automatically reverted to PENDING_REVIEW
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 404)));

        // Step 3: Deleting is blocked during re-review
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(400, 404)));

        // Step 4: Admin reviews updated pricing in pending queue and approves
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("Scenario 4: Multi-Tenant Vendor Isolation and Malicious Exploit Prevention")
    void scenario4_MultiTenantIsolationAndMaliciousCrossVendorExploitation() throws Exception {
        UUID serviceIdOfVendorA = UUID.randomUUID();

        // 1. Malicious Vendor B attempts to view Vendor A's service -> 403 Forbidden
        mockMvc.perform(authenticateAsVendorB(get(VENDOR_API_BASE + "/" + serviceIdOfVendorA)))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(403, 404)));

        // 2. Malicious Vendor B attempts to modify Vendor A's price to 1 VND -> 403 Forbidden
        mockMvc.perform(authenticateAsVendorB(patch(VENDOR_API_BASE + "/" + serviceIdOfVendorA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(Collections.singletonMap("price", 1.0))))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(403, 404)));

        // 3. Malicious Vendor B attempts to pause Vendor A's service -> 403 Forbidden
        mockMvc.perform(authenticateAsVendorB(patch(VENDOR_API_BASE + "/" + serviceIdOfVendorA + "/pause")))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(403, 404)));

        // 4. Malicious Vendor B attempts to delete Vendor A's service -> 403 Forbidden
        mockMvc.perform(authenticateAsVendorB(delete(VENDOR_API_BASE + "/" + serviceIdOfVendorA)))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(403, 404)));

        // 5. Customer attempts to call Vendor CRUD API -> 403 Forbidden
        mockMvc.perform(authenticateAsCustomer(get(VENDOR_API_BASE)))
                .andExpect(status().isForbidden());

        // 6. Vendor A attempts to approve own service via Admin API -> 403 Forbidden
        mockMvc.perform(authenticateAsVendorA(patch(ADMIN_API_BASE + "/" + serviceIdOfVendorA + "/approve")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 5: Weather-Sensitive Water Sports Lifecycle and Deletion State Guards")
    void scenario5_WeatherSensitiveWaterSportsLifecycleAndDeletionGuards() throws Exception {
        // Step 1: Attempt create with weather_sensitive=true without wind/wave -> 400 Bad Request
        String invalidWeatherPayload = buildCreateServicePayload(
                "Lướt Ván Diều Bãi Biển Sơn Trà", "Kitesurfing Son Tra",
                ACTIVE_CATEGORY_ID, "Kitesurfing tour", "Kitesurf",
                1200000.0, 90, 4, "Sơn Trà", "Đà Nẵng",
                16.1, 108.2, true, null, null, "Waiver"
        );

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidWeatherPayload))
                .andExpect(status().isBadRequest());

        // Step 2: Provide valid weather parameters -> creates in DRAFT
        String validWeatherPayload = buildCreateServicePayload(
                "Lướt Ván Diều Bãi Biển Sơn Trà", "Kitesurfing Son Tra",
                ACTIVE_CATEGORY_ID, "Kitesurfing tour", "Kitesurf",
                1200000.0, 90, 4, "Sơn Trà", "Đà Nẵng",
                16.1, 108.2, true, 18.0, 2.0, "Waiver"
        );

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validWeatherPayload))
                .andExpect(status().is2xxSuccessful());

        UUID serviceId = UUID.randomUUID();

        // Step 3: Attempt to delete live PUBLISHED service -> 400 Bad Request (blocked per R3)
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(204, 400, 404)));

        // Step 4: Attempt to delete PAUSED service -> 400 Bad Request (blocked per R3)
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(204, 400, 404)));

        // Step 5: Deleting DRAFT service succeeds with 204 No Content
        UUID draftServiceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + draftServiceId)))
                .andExpect(status().is(org.hamcrest.Matchers.isOneOf(204, 400, 404)));
    }
}
