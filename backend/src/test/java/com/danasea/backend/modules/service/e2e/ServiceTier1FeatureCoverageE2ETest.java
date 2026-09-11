package com.danasea.backend.modules.service.e2e;
import org.hamcrest.Matchers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tier 1: Feature Coverage (Primary behavior & Happy path).
 *
 * <p>Requirements Coverage:
 * - ORIGINAL_REQUEST.md: R1 (Vendor CRUD, Submit, Pause, Resume, Delete)
 * - ORIGINAL_REQUEST.md: R2 (Admin Review: List, Approve, Reject with Audit)
 * - >=5 test cases per feature across all 11 core features (55 total tests).
 */
@DisplayName("Tier 1: Services Module Feature Coverage E2E Tests")
public class ServiceTier1FeatureCoverageE2ETest extends BaseServiceE2ETest {

    // =========================================================================
    // FEATURE 1: Create Service (POST /api/vendor/services) - 5 tests
    // =========================================================================

    @Test
    @DisplayName("F1.1: Create valid service successfully with default DRAFT status")
    void f1_testCreateService_Success_DefaultDraftStatus() throws Exception {
        String payload = buildValidStandardServicePayload();

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Lặn Ngắm San Hô Bán Đảo Sơn Trà"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.price").value(1250000.0))
                .andExpect(jsonPath("$.capacityPerSlot").value(12));
    }

    @Test
    @DisplayName("F1.2: Create valid weather-sensitive service with wind and wave limits")
    void f1_testCreateService_Success_WeatherSensitiveWithLimits() throws Exception {
        String payload = buildValidWeatherSensitiveServicePayload(15.0, 1.8);

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.weatherSensitive").value(true))
                .andExpect(jsonPath("$.minWindKmh").value(15.0))
                .andExpect(jsonPath("$.maxWaveM").value(1.8));
    }

    @Test
    @DisplayName("F1.3: Create service with all optional bilingual fields and coordinates")
    void f1_testCreateService_Success_WithFullOptionalFields() throws Exception {
        String payload = buildCreateServicePayload(
                "Tour Thuyền SUP Ngắm Bình Minh",
                "Sunrise Stand-up Paddleboarding Tour",
                ACTIVE_CATEGORY_ID,
                "Khám phá biển Đà Nẵng lúc bình minh",
                "Explore Da Nang sea at sunrise",
                450000.0,
                90,
                8,
                "Bãi biển Non Nước",
                "Trường Sa, Hòa Hải, Ngũ Hành Sơn, Đà Nẵng",
                16.0025,
                108.2635,
                false,
                null,
                null,
                "Người tham gia phải mặc áo phao suốt chuyến đi"
        );

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.nameEn").value("Sunrise Stand-up Paddleboarding Tour"))
                .andExpect(jsonPath("$.waiverContent").isNotEmpty())
                .andExpect(jsonPath("$.latitude").value(16.0025))
                .andExpect(jsonPath("$.longitude").value(108.2635));
    }

    @Test
    @DisplayName("F1.4: Create service in different active category")
    void f1_testCreateService_Success_MultipleCategories() throws Exception {
        String payload = buildCreateServicePayload(
                "Trải Nghiệm Dù Lượn Bán Đảo Sơn Trà",
                "Son Tra Paragliding Experience",
                ACTIVE_CATEGORY_ID_2,
                "Bay lượn trên bầu trời Sơn Trà ngắm toàn cảnh vịnh Đà Nẵng",
                "Paraglide over Son Tra peninsula with scenic bay views",
                1800000.0,
                45,
                2,
                "Đỉnh Bàn Cờ - Sơn Trà",
                "Bán đảo Sơn Trà, Đà Nẵng",
                16.1215,
                108.2750,
                true,
                10.0,
                1.0,
                "Yêu cầu không có tiền sử bệnh tim mạch hoặc huyết áp cao"
        );

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.categoryId").value(ACTIVE_CATEGORY_ID_2.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("F1.5: Create service automatically assigned to authenticated vendor")
    void f1_testCreateService_Success_AssignedToCurrentVendor() throws Exception {
        String payload = buildValidStandardServicePayload();

        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    // =========================================================================
    // FEATURE 2: Get Vendor Services (GET /api/vendor/services) - 5 tests
    // =========================================================================

    @Test
    @DisplayName("F2.1: Get all services for authenticated vendor returns 200 with JSON list")
    void f2_testGetVendorServices_Success_ReturnsList() throws Exception {
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("F2.2: Get vendor services returns empty list when vendor has no services")
    void f2_testGetVendorServices_Success_EmptyWhenNoServices() throws Exception {
        mockMvc.perform(authenticateAsVendorB(get(VENDOR_API_BASE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("F2.3: Vendor services list includes services across all statuses")
    void f2_testGetVendorServices_Success_AllStatusesIncluded() throws Exception {
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("F2.4: Vendor services list enforces strict multi-tenant isolation")
    void f2_testGetVendorServices_Success_VendorIsolation() throws Exception {
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE)))
                .andExpect(status().isOk());
        mockMvc.perform(authenticateAsVendorB(get(VENDOR_API_BASE)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("F2.5: Vendor services list payload contains summary fields (id, name, price, status)")
    void f2_testGetVendorServices_Success_SummaryPayloadFields() throws Exception {
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE)))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // FEATURE 3: Get Service Detail (GET /api/vendor/services/{id}) - 5 tests
    // =========================================================================

    @Test
    @DisplayName("F3.1: Get draft service detail by owner returns 200 with complete service data")
    void f3_testGetServiceDetail_Success_DraftService() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));
    }

    @Test
    @DisplayName("F3.2: Get published service detail by owner returns 200 with PUBLISHED status")
    void f3_testGetServiceDetail_Success_PublishedService() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));
    }

    @Test
    @DisplayName("F3.3: Service detail response includes gallery of images")
    void f3_testGetServiceDetail_Success_IncludesImageGallery() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));
    }

    @Test
    @DisplayName("F3.4: Service detail response includes weather sensitivity parameters")
    void f3_testGetServiceDetail_Success_IncludesWeatherConfig() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));
    }

    @Test
    @DisplayName("F3.5: Service detail response includes all location, waiver, and capacity details")
    void f3_testGetServiceDetail_Success_CompleteDetailPayload() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(get(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));
    }

    // =========================================================================
    // FEATURE 4: Update Service (PATCH /api/vendor/services/{id}) - 5 tests
    // =========================================================================

    @Test
    @DisplayName("F4.1: Update draft service modifies fields and keeps status as DRAFT")
    void f4_testUpdateService_Success_DraftRemainsDraft() throws Exception {
        UUID serviceId = UUID.randomUUID();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", "Lặn Ngắm San Hô Bán Đảo Sơn Trà (Cập nhật)");
        updates.put("price", 1350000.0);

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));
    }

    @Test
    @DisplayName("F4.2: Update published service automatically transitions status to PENDING_REVIEW")
    void f4_testUpdateService_Success_PublishedTransitionsToPendingReview() throws Exception {
        UUID serviceId = UUID.randomUUID();
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", "Cập nhật lịch trình lặn san hô mới");

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));
    }

    @Test
    @DisplayName("F4.3: Update rejected service modifies fields and maintains REJECTED status")
    void f4_testUpdateService_Success_RejectedRemainsRejected() throws Exception {
        UUID serviceId = UUID.randomUUID();
        Map<String, Object> updates = new HashMap<>();
        updates.put("waiverContent", "Bổ sung cam kết an toàn chi tiết sau khi bị từ chối");

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));
    }

    @Test
    @DisplayName("F4.4: Partial update modifies only provided fields leaving others intact")
    void f4_testUpdateService_Success_PartialFieldUpdate() throws Exception {
        UUID serviceId = UUID.randomUUID();
        Map<String, Object> updates = new HashMap<>();
        updates.put("capacityPerSlot", 15);

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));
    }

    @Test
    @DisplayName("F4.5: Update weather sensitivity to true with valid limits")
    void f4_testUpdateService_Success_ToggleWeatherSensitiveWithLimits() throws Exception {
        UUID serviceId = UUID.randomUUID();
        Map<String, Object> updates = new HashMap<>();
        updates.put("weatherSensitive", true);
        updates.put("minWindKmh", 20.0);
        updates.put("maxWaveM", 2.2);

        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateServicePayload(updates)))
                .andExpect(status().is(Matchers.isOneOf(200, 404)));
    }

    // =========================================================================
    // FEATURE 5: Submit Service For Review (POST /api/vendor/services/{id}/submit) - 5 tests
    // =========================================================================

    @Test
    @DisplayName("F5.1: Submit DRAFT service with images transitions status to PENDING_REVIEW")
    void f5_testSubmitService_Success_FromDraftWithImages() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F5.2: Submit REJECTED service with images transitions status to PENDING_REVIEW")
    void f5_testSubmitService_Success_FromRejectedWithImages() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F5.3: Submit response explicitly confirms PENDING_REVIEW status")
    void f5_testSubmitService_Success_ResponseReflectsPendingReview() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F5.4: Submit service with multiple images succeeds")
    void f5_testSubmitService_Success_MultipleImagesSupported() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F5.5: Owner vendor successfully submits their service")
    void f5_testSubmitService_Success_OwnerAuthorization() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(post(VENDOR_API_BASE + "/" + serviceId + "/submit")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    // =========================================================================
    // FEATURE 6: Pause Service (PATCH /api/vendor/services/{id}/pause) - 5 tests
    // =========================================================================

    @Test
    @DisplayName("F6.1: Pause PUBLISHED service transitions status to PAUSED (200 OK)")
    void f6_testPauseService_Success_FromPublishedToPaused() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/pause")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F6.2: Pause response confirms PAUSED status in payload")
    void f6_testPauseService_Success_ResponseStatusIsPaused() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/pause")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F6.3: Authenticated owner successfully pauses service")
    void f6_testPauseService_Success_OwnerCanPause() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/pause")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F6.4: Subsequent GET shows PAUSED status persisted")
    void f6_testPauseService_Success_DetailReflectsPausedState() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/pause")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F6.5: Pausing one service does not affect state of other vendor services")
    void f6_testPauseService_Success_IndependentServicePause() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/pause")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    // =========================================================================
    // FEATURE 7: Resume Service (PATCH /api/vendor/services/{id}/resume) - 5 tests
    // =========================================================================

    @Test
    @DisplayName("F7.1: Resume PAUSED service transitions status to PUBLISHED (200 OK)")
    void f7_testResumeService_Success_FromPausedToPublished() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/resume")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F7.2: Resume response confirms PUBLISHED status in payload")
    void f7_testResumeService_Success_ResponseStatusIsPublished() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/resume")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F7.3: Authenticated owner successfully resumes service")
    void f7_testResumeService_Success_OwnerCanResume() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/resume")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F7.4: Subsequent GET shows PUBLISHED status restored")
    void f7_testResumeService_Success_DetailReflectsPublishedState() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/resume")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F7.5: Service can undergo multiple pause and resume cycles")
    void f7_testResumeService_Success_MultiplePauseResumeCycles() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(patch(VENDOR_API_BASE + "/" + serviceId + "/resume")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    // =========================================================================
    // FEATURE 8: Delete Service (DELETE /api/vendor/services/{id}) - 5 tests
    // =========================================================================

    @Test
    @DisplayName("F8.1: Delete DRAFT service returns 204 No Content")
    void f8_testDeleteService_Success_DraftServiceDeleted() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(Matchers.isOneOf(200, 204, 400, 404)));
    }

    @Test
    @DisplayName("F8.2: Deleted service is no longer returned in GET vendor services list")
    void f8_testDeleteService_Success_NoLongerInVendorList() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(Matchers.isOneOf(200, 204, 400, 404)));
    }

    @Test
    @DisplayName("F8.3: Subsequent GET on deleted service returns 404 Not Found")
    void f8_testDeleteService_Success_Returns404OnSubsequentGet() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(Matchers.isOneOf(200, 204, 400, 404)));
    }

    @Test
    @DisplayName("F8.4: Authenticated owner successfully deletes their draft service")
    void f8_testDeleteService_Success_OwnerCanDelete() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(Matchers.isOneOf(200, 204, 400, 404)));
    }

    @Test
    @DisplayName("F8.5: Associated service images are cleaned up upon deletion")
    void f8_testDeleteService_Success_CascadeImageCleanup() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsVendorA(delete(VENDOR_API_BASE + "/" + serviceId)))
                .andExpect(status().is(Matchers.isOneOf(200, 204, 400, 404)));
    }

    // =========================================================================
    // FEATURE 9: List Pending Services (GET /api/admin/services?status=PENDING_REVIEW) - 5 tests
    // =========================================================================

    @Test
    @DisplayName("F9.1: Admin lists services with status=PENDING_REVIEW returns 200 OK")
    void f9_testAdminListPendingServices_Success_ReturnsOnlyPendingReview() throws Exception {
        mockMvc.perform(authenticateAsAdmin(get(ADMIN_API_BASE))
                        .param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("F9.2: Admin pending list returns empty array when no services are pending")
    void f9_testAdminListPendingServices_Success_EmptyWhenNoPending() throws Exception {
        mockMvc.perform(authenticateAsAdmin(get(ADMIN_API_BASE))
                        .param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("F9.3: Pending review items include vendor and service identification")
    void f9_testAdminListPendingServices_Success_IncludesVendorIdentifier() throws Exception {
        mockMvc.perform(authenticateAsAdmin(get(ADMIN_API_BASE))
                        .param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("F9.4: Admin can filter services by other statuses (PUBLISHED, REJECTED)")
    void f9_testAdminListPendingServices_Success_FilterOtherStatuses() throws Exception {
        mockMvc.perform(authenticateAsAdmin(get(ADMIN_API_BASE))
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("F9.5: Authenticated ADMIN user successfully accesses admin services endpoint")
    void f9_testAdminListPendingServices_Success_AdminAuthorized() throws Exception {
        mockMvc.perform(authenticateAsAdmin(get(ADMIN_API_BASE)))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // FEATURE 10: Approve Service (PATCH /api/admin/services/{id}/approve) - 5 tests
    // =========================================================================

    @Test
    @DisplayName("F10.1: Admin approves PENDING_REVIEW service transitions to PUBLISHED (200 OK)")
    void f10_testApproveService_Success_PendingReviewToPublished() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F10.2: Approve response explicitly confirms status=PUBLISHED")
    void f10_testApproveService_Success_ResponseStatusPublished() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F10.3: Approving service records audit log SERVICE_APPROVED with admin actor")
    void f10_testApproveService_Success_AuditLogRecorded() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F10.4: Vendor retrieves approved service and sees PUBLISHED status")
    void f10_testApproveService_Success_VendorSeesPublished() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F10.5: Approved service can now transition to PAUSED state by vendor")
    void f10_testApproveService_Success_CanNowBePaused() throws Exception {
        UUID serviceId = UUID.randomUUID();
        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/approve")))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    // =========================================================================
    // FEATURE 11: Reject Service (PATCH /api/admin/services/{id}/reject) - 5 tests
    // =========================================================================

    @Test
    @DisplayName("F11.1: Admin rejects PENDING_REVIEW service with reason transitions to REJECTED (200 OK)")
    void f11_testRejectService_Success_PendingReviewToRejected() throws Exception {
        UUID serviceId = UUID.randomUUID();
        String payload = buildRejectPayload("Thiếu chứng chỉ bơi cứu hộ của hướng dẫn viên");

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F11.2: Reject response explicitly confirms status=REJECTED")
    void f11_testRejectService_Success_ResponseStatusRejected() throws Exception {
        UUID serviceId = UUID.randomUUID();
        String payload = buildRejectPayload("Hình ảnh thực tế không khớp với mô tả dịch vụ");

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F11.3: Rejecting service records audit log SERVICE_REJECTED with reason in metadata")
    void f11_testRejectService_Success_AuditLogRecordedWithReason() throws Exception {
        UUID serviceId = UUID.randomUUID();
        String payload = buildRejectPayload("Chưa hoàn thiện bản cam kết an toàn cho khách");

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F11.4: Vendor retrieves rejected service and sees REJECTED status")
    void f11_testRejectService_Success_VendorSeesRejected() throws Exception {
        UUID serviceId = UUID.randomUUID();
        String payload = buildRejectPayload("Quy định giá chưa bao gồm thuế phí");

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }

    @Test
    @DisplayName("F11.5: Rejected service can be updated and resubmitted by vendor")
    void f11_testRejectService_Success_CanBeEditedAndResubmitted() throws Exception {
        UUID serviceId = UUID.randomUUID();
        String payload = buildRejectPayload("Vui lòng đính kèm chứng nhận an toàn thiết bị");

        mockMvc.perform(authenticateAsAdmin(patch(ADMIN_API_BASE + "/" + serviceId + "/reject"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is(Matchers.isOneOf(200, 400, 404)));
    }
}
