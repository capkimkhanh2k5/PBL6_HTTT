package com.danasea.backend.modules.report.integration;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.danasea.backend.modules.report.application.usecases.ExportReportCsvUseCase;
import com.danasea.backend.modules.report.application.usecases.GetAdminDashboardUseCase;
import com.danasea.backend.modules.report.application.usecases.GetBookingReportUseCase;
import com.danasea.backend.modules.report.application.usecases.GetRevenueReportUseCase;
import com.danasea.backend.modules.report.application.usecases.GetVendorDashboardUseCase;
import com.danasea.backend.modules.report.application.usecases.GetVendorPerformanceReportUseCase;
import com.danasea.backend.modules.report.domain.enums.GroupByPeriod;
import com.danasea.backend.modules.report.domain.enums.ReportType;
import com.danasea.backend.modules.report.domain.models.DashboardMetrics;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.security.authentication.application.ports.UserAccountPort;
import com.danasea.backend.security.authentication.domain.models.Authentication;
import com.danasea.backend.security.authentication.infrastructure.security.JwtTokenProvider;
import com.danasea.backend.security.authorization.BaseSecurityIntegrationTest;
import com.danasea.backend.security.authorization.application.ports.AuthorizationPort;
import com.danasea.backend.security.authorization.domain.models.AuthorizationSubject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adversarial Security & Vendor Isolation Integration Test Suite for Module Report.
 *
 * <p>Verifies real Spring Security Filter Chain, Path-based Authorization, Method Security
 * (@PreAuthorize), IDOR immunity, and Vendor Isolation against malicious callers.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ReportSecurityAdversarialIntegrationTest extends BaseSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private UserAccountPort userAccountPort;

    @MockitoBean private AuthorizationPort authorizationPort;

    @MockitoBean private VendorInternalApi vendorInternalApi;

    @MockitoBean private GetAdminDashboardUseCase getAdminDashboardUseCase;

    @MockitoBean private GetVendorDashboardUseCase getVendorDashboardUseCase;

    @MockitoBean private GetRevenueReportUseCase getRevenueReportUseCase;

    @MockitoBean private GetBookingReportUseCase getBookingReportUseCase;

    @MockitoBean private GetVendorPerformanceReportUseCase getVendorPerformanceReportUseCase;

    @MockitoBean private ExportReportCsvUseCase exportReportCsvUseCase;

    private String createAuthenticatedUser(UUID userId, String email, String role) {
        Authentication auth = new Authentication(userId, email, "hashed_pw", role, true, true);
        when(userAccountPort.findByEmail(email)).thenReturn(Optional.of(auth));
        when(authorizationPort.findSubjectByEmail(email))
                .thenReturn(
                        new AuthorizationSubject(
                                userId, email, Set.of(role), Set.of("READ", "WRITE")));
        return jwtTokenProvider.generateAccessToken(auth);
    }

    private Vendor mockVendorProfile(UUID userId, UUID vendorId, String businessName) {
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setUserId(userId);
        vendor.setBusinessName(businessName);
        when(vendorInternalApi.findByUserId(userId)).thenReturn(Optional.of(vendor));
        return vendor;
    }

    // =========================================================================
    // 1. UNFAIR / UNAUTHENTICATED ACCESS ATTEMPTS -> 401 UNAUTHORIZED
    // =========================================================================

    @Test
    @DisplayName(
            "[Adversarial 1.1] Unauthenticated caller accessing /api/vendor/* endpoints must be"
                    + " rejected with 401 Unauthorized")
    void unauthenticatedCaller_vendorEndpoints_returns401() throws Exception {
        mockMvc.perform(get("/api/vendor/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(
                        get("/api/vendor/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(
                        get("/api/vendor/reports/bookings")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(
                        get("/api/vendor/reports/export")
                                .param("type", "revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName(
            "[Adversarial 1.2] Unauthenticated caller accessing /api/admin/reports/* endpoints must"
                    + " be rejected with 401 Unauthorized")
    void unauthenticatedCaller_adminEndpoints_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(
                        get("/api/admin/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(
                        get("/api/admin/reports/bookings")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(
                        get("/api/admin/reports/vendors")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(
                        get("/api/admin/reports/export")
                                .param("type", "revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // =========================================================================
    // 2. CROSS-ROLE ACCESS: CUSTOMER -> VENDOR / ADMIN ENDPOINTS -> 403 FORBIDDEN
    // =========================================================================

    @Test
    @DisplayName(
            "[Adversarial 2.1] CUSTOMER role calling /api/vendor/* endpoints must be blocked by"
                    + " @PreAuthorize with 403 Forbidden")
    void customerRole_accessingVendorEndpoints_returns403() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token =
                createAuthenticatedUser(customerId, "customer_attacker@danasea.vn", "CUSTOMER");

        mockMvc.perform(get("/api/vendor/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(
                        get("/api/vendor/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(
                        get("/api/vendor/reports/bookings")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(
                        get("/api/vendor/reports/export")
                                .param("type", "revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName(
            "[Adversarial 2.2] CUSTOMER role calling /api/admin/* endpoints must be blocked by"
                    + " Security Filter Chain with 403 Forbidden")
    void customerRole_accessingAdminEndpoints_returns403() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token =
                createAuthenticatedUser(customerId, "customer_attacker@danasea.vn", "CUSTOMER");

        mockMvc.perform(
                        get("/api/admin/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(
                        get("/api/admin/reports/bookings")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(
                        get("/api/admin/reports/vendors")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(
                        get("/api/admin/reports/export")
                                .param("type", "revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    // =========================================================================
    // 3. CROSS-ROLE ACCESS: VENDOR -> ADMIN ENDPOINTS -> 403 FORBIDDEN
    // =========================================================================

    @Test
    @DisplayName(
            "[Adversarial 3.1] VENDOR role calling /api/admin/* endpoints must be blocked by"
                    + " Security Filter Chain with 403 Forbidden")
    void vendorRole_accessingAdminEndpoints_returns403() throws Exception {
        UUID vendorUserId = UUID.randomUUID();
        String token = createAuthenticatedUser(vendorUserId, "vendor_user@danasea.vn", "VENDOR");

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(
                        get("/api/admin/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(
                        get("/api/admin/reports/bookings")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(
                        get("/api/admin/reports/vendors")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(
                        get("/api/admin/reports/export")
                                .param("type", "revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    // =========================================================================
    // 4. IDOR ATTACKS: VENDOR ATTEMPTS TO TAMPER WITH ?vendorId PARAMETER
    // =========================================================================

    @Test
    @DisplayName(
            "[Adversarial 4.1] Vendor IDOR attack on GET /api/vendor/dashboard is neutralized:"
                    + " vendorId param ignored")
    void vendorIdorAttack_dashboard_neutralized() throws Exception {
        UUID myUserId = UUID.randomUUID();
        UUID myVendorId = UUID.randomUUID();
        UUID victimVendorId = UUID.randomUUID();

        String token = createAuthenticatedUser(myUserId, "legit_vendor@danasea.vn", "VENDOR");
        mockVendorProfile(myUserId, myVendorId, "My Legit Business");

        when(getVendorDashboardUseCase.execute(eq(myVendorId), any(TimeRange.class)))
                .thenReturn(DashboardMetrics.empty());

        mockMvc.perform(
                        get("/api/vendor/dashboard")
                                .param("vendorId", victimVendorId.toString())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verify UseCase is strictly invoked with caller's own vendorId, NEVER victim's
        verify(getVendorDashboardUseCase).execute(eq(myVendorId), any(TimeRange.class));
        verify(getVendorDashboardUseCase, never())
                .execute(eq(victimVendorId), any(TimeRange.class));
    }

    @Test
    @DisplayName(
            "[Adversarial 4.2] Vendor IDOR attack on GET /api/vendor/reports/revenue is"
                    + " neutralized: vendorId param ignored")
    void vendorIdorAttack_revenueReport_neutralized() throws Exception {
        UUID myUserId = UUID.randomUUID();
        UUID myVendorId = UUID.randomUUID();
        UUID victimVendorId = UUID.randomUUID();

        String token = createAuthenticatedUser(myUserId, "legit_vendor@danasea.vn", "VENDOR");
        mockVendorProfile(myUserId, myVendorId, "My Legit Business");

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 5);
        TimeRange expectedRange = TimeRange.of(from, to);

        when(getRevenueReportUseCase.execute(
                        eq(expectedRange), eq(GroupByPeriod.DAY), eq(myVendorId)))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/vendor/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .param("groupBy", "day")
                                .param("vendorId", victimVendorId.toString())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(getRevenueReportUseCase)
                .execute(eq(expectedRange), eq(GroupByPeriod.DAY), eq(myVendorId));
        verify(getRevenueReportUseCase, never())
                .execute(eq(expectedRange), eq(GroupByPeriod.DAY), eq(victimVendorId));
    }

    @Test
    @DisplayName(
            "[Adversarial 4.3] Vendor IDOR attack on GET /api/vendor/reports/bookings is"
                    + " neutralized: vendorId param ignored")
    void vendorIdorAttack_bookingReport_neutralized() throws Exception {
        UUID myUserId = UUID.randomUUID();
        UUID myVendorId = UUID.randomUUID();
        UUID victimVendorId = UUID.randomUUID();

        String token = createAuthenticatedUser(myUserId, "legit_vendor@danasea.vn", "VENDOR");
        mockVendorProfile(myUserId, myVendorId, "My Legit Business");

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 5);
        TimeRange expectedRange = TimeRange.of(from, to);

        when(getBookingReportUseCase.execute(
                        eq(expectedRange), eq(GroupByPeriod.DAY), eq(myVendorId)))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/vendor/reports/bookings")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .param("groupBy", "day")
                                .param("vendorId", victimVendorId.toString())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(getBookingReportUseCase)
                .execute(eq(expectedRange), eq(GroupByPeriod.DAY), eq(myVendorId));
        verify(getBookingReportUseCase, never())
                .execute(eq(expectedRange), eq(GroupByPeriod.DAY), eq(victimVendorId));
    }

    @Test
    @DisplayName(
            "[Adversarial 4.4] Vendor IDOR attack on GET /api/vendor/reports/export is neutralized:"
                    + " vendorId param ignored")
    void vendorIdorAttack_exportReport_neutralized() throws Exception {
        UUID myUserId = UUID.randomUUID();
        UUID myVendorId = UUID.randomUUID();
        UUID victimVendorId = UUID.randomUUID();

        String token = createAuthenticatedUser(myUserId, "legit_vendor@danasea.vn", "VENDOR");
        mockVendorProfile(myUserId, myVendorId, "My Legit Business");

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 5);
        TimeRange expectedRange = TimeRange.of(from, to);

        byte[] csvBytes = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'O', 'K'};
        when(exportReportCsvUseCase.execute(
                        eq(ReportType.REVENUE),
                        eq(expectedRange),
                        eq(GroupByPeriod.DAY),
                        eq(myVendorId)))
                .thenReturn(csvBytes);

        mockMvc.perform(
                        get("/api/vendor/reports/export")
                                .param("type", "revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .param("vendorId", victimVendorId.toString())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, is("text/csv;charset=UTF-8")));

        verify(exportReportCsvUseCase)
                .execute(
                        eq(ReportType.REVENUE),
                        eq(expectedRange),
                        eq(GroupByPeriod.DAY),
                        eq(myVendorId));
        verify(exportReportCsvUseCase, never())
                .execute(
                        eq(ReportType.REVENUE),
                        eq(expectedRange),
                        eq(GroupByPeriod.DAY),
                        eq(victimVendorId));
    }

    // =========================================================================
    // 5. ORPHAN VENDOR ACCOUNT (ROLE_VENDOR but NO VENDOR PROFILE IN DB) -> 403
    // =========================================================================

    @Test
    @DisplayName(
            "[Adversarial 5.1] Authenticated VENDOR without vendor profile in DB is denied with 403"
                    + " ACCESS_DENIED")
    void orphanVendorUser_accessingVendorEndpoints_returns403() throws Exception {
        UUID orphanUserId = UUID.randomUUID();
        String token = createAuthenticatedUser(orphanUserId, "orphan_vendor@danasea.vn", "VENDOR");

        when(vendorInternalApi.findByUserId(orphanUserId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/vendor/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(
                        get("/api/vendor/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    // =========================================================================
    // 6. ADMIN LEGITIMATE ACCESS (WHOLESALE VS FILTERED)
    // =========================================================================

    @Test
    @DisplayName(
            "[Adversarial 6.1] ADMIN can view wholesale reports or optionally filter by vendorId")
    void adminUser_canAccessWholeSystemOrFiltered() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        String token = createAuthenticatedUser(adminUserId, "admin_user@danasea.vn", "ADMIN");

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 5);
        TimeRange expectedRange = TimeRange.of(from, to);

        UUID filterVendorId = UUID.randomUUID();

        // 1. Wholesale (vendorId = null)
        when(getRevenueReportUseCase.execute(eq(expectedRange), eq(GroupByPeriod.DAY), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/admin/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(getRevenueReportUseCase).execute(eq(expectedRange), eq(GroupByPeriod.DAY), isNull());

        // 2. Filtered by vendorId
        when(getRevenueReportUseCase.execute(
                        eq(expectedRange), eq(GroupByPeriod.DAY), eq(filterVendorId)))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/admin/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .param("vendorId", filterVendorId.toString())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(getRevenueReportUseCase)
                .execute(eq(expectedRange), eq(GroupByPeriod.DAY), eq(filterVendorId));
    }
}
