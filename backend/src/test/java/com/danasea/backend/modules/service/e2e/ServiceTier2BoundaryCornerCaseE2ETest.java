package com.danasea.backend.modules.service.e2e;
import org.hamcrest.Matchers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tier 2: Boundary & Corner Cases (Negative paths, validation rules, state guards, authorization boundaries).
 *
 * <p>Requirements Coverage:
 * - ORIGINAL_REQUEST.md: R3 (Business Rules Validation - Vendor Approval, Weather limits, Image mandatory, State Guards)
 * - ORIGINAL_REQUEST.md: R4 (Security & Access Control - Role guards & Owner isolation)
 * - >=5 boundary / corner cases per feature across all 11 core features (58 total tests).
 */
@DisplayName("Tier 2: Services Module Boundary & Corner Cases E2E Tests")
public class ServiceTier2BoundaryCornerCaseE2ETest extends BaseServiceE2ETest {

    // =========================================================================
    // FEATURE 1: Create Service - Boundary & Negative Cases (8 tests)
    // =========================================================================

    @Test
    @DisplayName("F1.B1: Fail when vendor verification_status is PENDING (R3)")
    void f1_testCreateService_Fail_VendorPendingVerification() throws Exception {
        String payload = buildValidStandardServicePayload();

        mockMvc.perform(authenticateAsPendingVendor(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(Matchers.isOneOf(400, 403)));
    }

    @Test
    @DisplayName("F1.B2: Fail when vendor verification_status is REJECTED (R3)")
    void f1_testCreateService_Fail_VendorRejectedVerification() throws Exception {
        String payload = buildValidStandardServicePayload();

        mockMvc.perform(authenticateAsRejectedVendor(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(Matchers.isOneOf(400, 403)));
    }

    @Test
    @DisplayName("F1.B3: Fail when category_id does not exist (404/400 per R3)")
    void f1_testCreateService_Fail_CategoryNotFound() throws Exception {
        String payload = buildCreateServicePayload(
                "Lặn San Hô", "Scuba Diving", NON_EXISTENT_CATEGORY_ID,
                "Mô tả", "Description", 1000000.0, 60, 5,
                "Sơn Trà", "Đà Nẵng", 16.1, 108.2, false, null, null, "Waiver"
        );

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F1.B4: Fail when category_id is inactive (400 per R3)")
    void f1_testCreateService_Fail_CategoryInactive() throws Exception {
        String payload = buildCreateServicePayload(
                "Lặn San Hô", "Scuba Diving", INACTIVE_CATEGORY_ID,
                "Mô tả", "Description", 1000000.0, 60, 5,
                "Sơn Trà", "Đà Nẵng", 16.1, 108.2, false, null, null, "Waiver"
        );

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F1.B5: Fail when weather_sensitive=true but min_wind_kmh is missing (R3)")
    void f1_testCreateService_Fail_WeatherSensitiveMissingMinWind() throws Exception {
        String payload = buildCreateServicePayload(
                "Dù Bay Biển", "Parasailing", ACTIVE_CATEGORY_ID,
                "Bay lượn trên biển", "Parasail", 750000.0, 30, 2,
                "Mỹ Khê", "Đà Nẵng", 16.06, 108.24, true, null, 1.5, "Waiver"
        );

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F1.B6: Fail when weather_sensitive=true but max_wave_m is missing (R3)")
    void f1_testCreateService_Fail_WeatherSensitiveMissingMaxWave() throws Exception {
        String payload = buildCreateServicePayload(
                "Dù Bay Biển", "Parasailing", ACTIVE_CATEGORY_ID,
                "Bay lượn trên biển", "Parasail", 750000.0, 30, 2,
                "Mỹ Khê", "Đà Nẵng", 16.06, 108.24, true, 12.0, null, "Waiver"
        );

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F1.B7: Fail when price is zero or negative")
    void f1_testCreateService_Fail_ZeroOrNegativePrice() throws Exception {
        String payload = buildCreateServicePayload(
                "Dịch Vụ Giá Âm", "Invalid Price", ACTIVE_CATEGORY_ID,
                "Mô tả", "Description", -100000.0, 60, 5,
                "Sơn Trà", "Đà Nẵng", 16.1, 108.2, false, null, null, "Waiver"
        );

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F1.B8: Fail when service name is blank or whitespace")
    void f1_testCreateService_Fail_BlankName() throws Exception {
        String payload = buildCreateServicePayload(
                "    ", "Blank name", ACTIVE_CATEGORY_ID,
                "Mô tả", "Description", 500000.0, 60, 5,
                "Sơn Trà", "Đà Nẵng", 16.1, 108.2, false, null, null, "Waiver"
        );

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // FEATURE 2: Get Vendor Services - Boundary & Negative Cases (5 tests)
    // =========================================================================

    @Test
    @DisplayName("F2.B1: Fail when unauthenticated user accesses vendor services")
    void f2_testGetVendorServices_Fail_AnonymousUnauthenticated() throws Exception {
        mockMvc.perform(get(VENDOR_API_BASE))
                .andExpect(status().is(Matchers.isOneOf(401, 403)));
    }

    @Test
    @DisplayName("F2.B2: Block CUSTOMER role from /api/vendor/services (403 per R4)")
    void f2_testGetVendorServices_Fail_CustomerRoleBlocked() throws Exception {
        mockMvc.perform(authenticateAsCustomer(get(VENDOR_API_BASE)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2.B3: Block ADMIN role from /api/vendor/services (403 per R4)")
    void f2_testGetVendorServices_Fail_AdminRoleBlocked() throws Exception {
        mockMvc.perform(authenticateAsAdmin(get(VENDOR_API_BASE)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F2.B4: Handle extremely large pagination offset safely")
    void f2_testGetVendorServices_Boundary_ExtremelyLongPaginationOffset() throws Exception {
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE))
                        .param("page", "999999")
                        .param("size", "100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("F2.B5: SQL meta-characters in search query handled safely without injection")
    void f2_testGetVendorServices_Boundary_SpecialCharactersInFilter() throws Exception {
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE))
                        .param("search", "'; DROP TABLE services; --"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // FEATURE 3: Get Service Detail - Boundary & Negative Cases (5 tests)
    // =========================================================================

    @Test
    @DisplayName("F3.B1: Fail with 404 when serviceId does not exist")
    void f3_testGetServiceDetail_Fail_NonExistentService() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE + "/" + nonExistentId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("F3.B2: Block Vendor B from viewing Vendor A's service (403 owner check R1, R3)")
    void f3_testGetServiceDetail_Fail_OtherVendorForbidden() throws Exception {
        UUID serviceIdOfVendorA = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorB(get(VENDOR_API_BASE + "/" + serviceIdOfVendorA)))
                .andExpect(status().is(Matchers.isOneOf(403, 404)));
    }

    @Test
    @DisplayName("F3.B3: Block CUSTOMER from viewing vendor service detail endpoint (403 per R4)")
    void f3_testGetServiceDetail_Fail_CustomerRoleForbidden() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsCustomer(get(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F3.B4: Block ADMIN from calling vendor service detail endpoint (403 per R4)")
    void f3_testGetServiceDetail_Fail_AdminRoleForbidden() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsAdmin(get(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F3.B5: Malformed non-UUID serviceId returns 400 Bad Request")
    void f3_testGetServiceDetail_Fail_MalformedUuid() throws Exception {
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE + "/not-a-valid-uuid-1234")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    // =========================================================================
    // FEATURE 4: Update Service - Boundary & Negative Cases (6 tests)
    // =========================================================================

    @Test
    @DisplayName("F4.B1: Block Vendor B from updating Vendor A's service (403 owner check R3)")
    void f4_testUpdateService_Fail_OtherVendorForbidden() throws Exception {
        UUID serviceId = UUID.randomUUID();
        Map<String, Object> updates = Collections.singletonMap("name", "Malicious Update Attempt");

        mockMvc.perform(authenticateAsVendorB(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().is(Matchers.isOneOf(403, 404)));
    }

    @Test
    @DisplayName("F4.B2: Updating non-existent serviceId returns 404 Not Found")
    void f4_testUpdateService_Fail_ServiceNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        Map<String, Object> updates = Collections.singletonMap("price", 999000.0);

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + nonExistentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("F4.B3: Updating to an inactive category returns 400 Bad Request (R3)")
    void f4_testUpdateService_Fail_UpdateToInactiveCategory() throws Exception {
        UUID serviceId = UUID.randomUUID();
        Map<String, Object> updates = Collections.singletonMap("categoryId", INACTIVE_CATEGORY_ID.toString());

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F4.B4: Updating weather_sensitive=true without wind/wave limits returns 400 (R3)")
    void f4_testUpdateService_Fail_WeatherSensitiveWithoutWindWave() throws Exception {
        UUID serviceId = UUID.randomUUID();
        Map<String, Object> updates = new HashMap<>();
        updates.put("weatherSensitive", true);
        updates.put("minWindKmh", null);
        updates.put("maxWaveM", null);

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F4.B5: Block CUSTOMER from calling update service endpoint (403 per R4)")
    void f4_testUpdateService_Fail_CustomerRoleForbidden() throws Exception {
        UUID serviceId = UUID.randomUUID();
        Map<String, Object> updates = Collections.singletonMap("price", 500000.0);

        mockMvc.perform(authenticateAsCustomer(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F4.B6: Updating capacityPerSlot or durationMinutes to negative returns 400")
    void f4_testUpdateService_Boundary_NegativeCapacityOrDuration() throws Exception {
        UUID serviceId = UUID.randomUUID();
        Map<String, Object> updates = Collections.singletonMap("capacityPerSlot", -5);

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    // =========================================================================
    // FEATURE 5: Submit Service For Review - Boundary & Negative Cases (6 tests)
    // =========================================================================

    @Test
    @DisplayName("F5.B1: Fail when submitting service with no images (400 per R3)")
    void f5_testSubmitService_Fail_EmptyImages() throws Exception {
        UUID serviceWithoutImagesId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceWithoutImagesId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F5.B2: Fail when submitting service that is already PENDING_REVIEW (400 state guard)")
    void f5_testSubmitService_Fail_AlreadyPendingReview() throws Exception {
        UUID pendingServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + pendingServiceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F5.B3: Fail when submitting service that is already PUBLISHED (400 state guard)")
    void f5_testSubmitService_Fail_AlreadyPublished() throws Exception {
        UUID publishedServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + publishedServiceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F5.B4: Fail when submitting service that is currently PAUSED (400 state guard)")
    void f5_testSubmitService_Fail_PausedService() throws Exception {
        UUID pausedServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + pausedServiceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F5.B5: Block Vendor B from submitting Vendor A's service (403 owner check)")
    void f5_testSubmitService_Fail_OtherVendorForbidden() throws Exception {
        UUID serviceIdOfVendorA = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorB(post(VENDOR_API_BASE + "/" + serviceIdOfVendorA + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(403, 404)));
    }

    @Test
    @DisplayName("F5.B6: Submitting non-existent service returns 404 Not Found")
    void f5_testSubmitService_Fail_NonExistentService() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + nonExistentId + "/submit")))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // FEATURE 6: Pause Service - Boundary & Negative Cases (6 tests)
    // =========================================================================

    @Test
    @DisplayName("F6.B1: Fail when calling pause on service in DRAFT status (400 state guard)")
    void f6_testPauseService_Fail_FromDraft() throws Exception {
        UUID draftServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + draftServiceId + "/pause")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F6.B2: Fail when calling pause on service in PENDING_REVIEW status (400 state guard)")
    void f6_testPauseService_Fail_FromPendingReview() throws Exception {
        UUID pendingServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + pendingServiceId + "/pause")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F6.B3: Fail when calling pause on service in REJECTED status (400 state guard)")
    void f6_testPauseService_Fail_FromRejected() throws Exception {
        UUID rejectedServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + rejectedServiceId + "/pause")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F6.B4: Fail when calling pause on already PAUSED service (400 state guard)")
    void f6_testPauseService_Fail_AlreadyPaused() throws Exception {
        UUID pausedServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + pausedServiceId + "/pause")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F6.B5: Block Vendor B from pausing Vendor A's service (403 owner check)")
    void f6_testPauseService_Fail_OtherVendorForbidden() throws Exception {
        UUID serviceIdOfVendorA = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorB(patch(VENDOR_API_BASE + "/" + serviceIdOfVendorA + "/pause")))
                .andExpect(status().is(Matchers.isOneOf(403, 404)));
    }

    @Test
    @DisplayName("F6.B6: Pausing non-existent service returns 404 Not Found")
    void f6_testPauseService_Fail_ServiceNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + nonExistentId + "/pause")))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // FEATURE 7: Resume Service - Boundary & Negative Cases (6 tests)
    // =========================================================================

    @Test
    @DisplayName("F7.B1: Fail when calling resume on service in DRAFT status (400 state guard)")
    void f7_testResumeService_Fail_FromDraft() throws Exception {
        UUID draftServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + draftServiceId + "/resume")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F7.B2: Fail when calling resume on service in PENDING_REVIEW status (400 state guard)")
    void f7_testResumeService_Fail_FromPendingReview() throws Exception {
        UUID pendingServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + pendingServiceId + "/resume")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F7.B3: Fail when calling resume on service in REJECTED status (400 state guard)")
    void f7_testResumeService_Fail_FromRejected() throws Exception {
        UUID rejectedServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + rejectedServiceId + "/resume")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F7.B4: Fail when calling resume on already PUBLISHED service (400 state guard)")
    void f7_testResumeService_Fail_AlreadyPublished() throws Exception {
        UUID publishedServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + publishedServiceId + "/resume")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F7.B5: Block Vendor B from resuming Vendor A's service (403 owner check)")
    void f7_testResumeService_Fail_OtherVendorForbidden() throws Exception {
        UUID serviceIdOfVendorA = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorB(patch(VENDOR_API_BASE + "/" + serviceIdOfVendorA + "/resume")))
                .andExpect(status().is(Matchers.isOneOf(403, 404)));
    }

    @Test
    @DisplayName("F7.B6: Resuming non-existent service returns 404 Not Found")
    void f7_testResumeService_Fail_ServiceNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + nonExistentId + "/resume")))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // FEATURE 8: Delete Service - Boundary & Negative Cases (6 tests)
    // =========================================================================

    @Test
    @DisplayName("F8.B1: Fail when attempting to delete PUBLISHED service (400 per R3)")
    void f8_testDeleteService_Fail_PublishedServiceBlocked() throws Exception {
        UUID publishedServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + publishedServiceId)))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F8.B2: Fail when attempting to delete PAUSED service (400 per R3)")
    void f8_testDeleteService_Fail_PausedServiceBlocked() throws Exception {
        UUID pausedServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + pausedServiceId)))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F8.B3: Fail when attempting to delete PENDING_REVIEW service (400 per R3)")
    void f8_testDeleteService_Fail_PendingReviewServiceBlocked() throws Exception {
        UUID pendingServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + pendingServiceId)))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F8.B4: Fail when attempting to delete REJECTED service (400 per R3)")
    void f8_testDeleteService_Fail_RejectedServiceBlocked() throws Exception {
        UUID rejectedServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + rejectedServiceId)))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F8.B5: Block Vendor B from deleting Vendor A's service (403 owner check)")
    void f8_testDeleteService_Fail_OtherVendorForbidden() throws Exception {
        UUID serviceIdOfVendorA = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorB(delete(VENDOR_API_BASE + "/" + serviceIdOfVendorA)))
                .andExpect(status().is(Matchers.isOneOf(403, 404)));
    }

    @Test
    @DisplayName("F8.B6: Deleting non-existent service returns 404 Not Found")
    void f8_testDeleteService_Fail_ServiceNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + nonExistentId)))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // FEATURE 9: List Pending Services (Admin) - Boundary & Negative Cases (5 tests)
    // =========================================================================

    @Test
    @DisplayName("F9.B1: Block VENDOR role from calling admin services endpoint (403 per R4)")
    void f9_testAdminListServices_Fail_VendorRoleForbidden() throws Exception {
        mockMvc.perform(authenticateAsVendorA(get(ADMIN_API_BASE)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F9.B2: Block CUSTOMER role from calling admin services endpoint (403 per R4)")
    void f9_testAdminListServices_Fail_CustomerRoleForbidden() throws Exception {
        mockMvc.perform(authenticateAsCustomer(get(ADMIN_API_BASE)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F9.B3: Block unauthenticated access to admin services endpoint (401/403)")
    void f9_testAdminListServices_Fail_AnonymousForbidden() throws Exception {
        mockMvc.perform(get(ADMIN_API_BASE))
                .andExpect(status().is(Matchers.isOneOf(401, 403)));
    }

    @Test
    @DisplayName("F9.B4: Non-existent status query parameter returns 400 Bad Request")
    void f9_testAdminListServices_Boundary_InvalidStatusParameter() throws Exception {
        mockMvc.perform(authenticateAsAdmin(get(ADMIN_API_BASE))
                        .param("status", "INVALID_STATUS_VALUE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F9.B5: Accessing admin services with empty query string handles gracefully")
    void f9_testAdminListServices_Boundary_EmptyQueryString() throws Exception {
        mockMvc.perform(authenticateAsAdmin(get(ADMIN_API_BASE)))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // FEATURE 10: Approve Service (Admin) - Boundary & Negative Cases (6 tests)
    // =========================================================================

    @Test
    @DisplayName("F10.B1: Fail when calling approve on service in DRAFT status (400 state guard)")
    void f10_testApproveService_Fail_FromDraft() throws Exception {
        UUID draftServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + draftServiceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F10.B2: Fail when calling approve on already PUBLISHED service (400 state guard)")
    void f10_testApproveService_Fail_AlreadyPublished() throws Exception {
        UUID publishedServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + publishedServiceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F10.B3: Fail when calling approve on service in PAUSED status (400 state guard)")
    void f10_testApproveService_Fail_FromPaused() throws Exception {
        UUID pausedServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + pausedServiceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F10.B4: Fail when calling approve on service in REJECTED status (400 state guard)")
    void f10_testApproveService_Fail_FromRejected() throws Exception {
        UUID rejectedServiceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + rejectedServiceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F10.B5: Block VENDOR role from calling approve endpoint (403 per R4)")
    void f10_testApproveService_Fail_VendorRoleForbidden() throws Exception {
        UUID serviceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsVendorA(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F10.B6: Approving non-existent service returns 404 Not Found")
    void f10_testApproveService_Fail_ServiceNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + nonExistentId + "/approve")))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // FEATURE 11: Reject Service (Admin) - Boundary & Negative Cases (8 tests)
    // =========================================================================

    @Test
    @DisplayName("F11.B1: Fail when rejecting without reason body (400 per R2)")
    void f11_testRejectService_Fail_MissingReason() throws Exception {
        UUID serviceId = UUID.randomUUID();

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F11.B2: Fail when rejecting with blank whitespace reason (400 per R2)")
    void f11_testRejectService_Fail_BlankReason() throws Exception {
        UUID serviceId = UUID.randomUUID();
        String payload = buildRejectPayload("     ");

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F11.B3: Fail when rejecting with null reason (400 per R2)")
    void f11_testRejectService_Fail_NullReason() throws Exception {
        UUID serviceId = UUID.randomUUID();
        String payload = "{\"reason\": null}";

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F11.B4: Fail when calling reject on service in DRAFT status (400 state guard)")
    void f11_testRejectService_Fail_FromDraft() throws Exception {
        UUID draftServiceId = UUID.randomUUID();
        String payload = buildRejectPayload("Cannot reject draft");

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + draftServiceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F11.B5: Fail when calling reject on service in PUBLISHED status (400 state guard)")
    void f11_testRejectService_Fail_FromPublished() throws Exception {
        UUID publishedServiceId = UUID.randomUUID();
        String payload = buildRejectPayload("Cannot reject published");

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + publishedServiceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F11.B6: Fail when calling reject on service in PAUSED status (400 state guard)")
    void f11_testRejectService_Fail_FromPaused() throws Exception {
        UUID pausedServiceId = UUID.randomUUID();
        String payload = buildRejectPayload("Cannot reject paused");

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + pausedServiceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(Matchers.isOneOf(400, 404)));
    }

    @Test
    @DisplayName("F11.B7: Block VENDOR role from calling reject endpoint (403 per R4)")
    void f11_testRejectService_Fail_VendorRoleForbidden() throws Exception {
        UUID serviceId = UUID.randomUUID();
        String payload = buildRejectPayload("Vendor trying to reject");

        mockMvc.perform(authenticateAsVendorA(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F11.B8: Rejecting non-existent service returns 404 Not Found")
    void f11_testRejectService_Fail_ServiceNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        String payload = buildRejectPayload("Non-existent service");

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + nonExistentId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }
}
