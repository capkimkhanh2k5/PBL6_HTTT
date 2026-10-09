package com.danasea.backend.modules.report.presentation;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.danasea.backend.modules.report.application.usecases.GetVendorDashboardUseCase;
import com.danasea.backend.modules.report.domain.enums.VendorAlertSeverity;
import com.danasea.backend.modules.report.domain.enums.VendorIssueType;
import com.danasea.backend.modules.report.domain.models.DashboardMetrics;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorDiagnosisAlert;
import com.danasea.backend.modules.report.presentation.controllers.VendorDashboardController;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.shared.presentation.GlobalExceptionHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class VendorDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock private GetVendorDashboardUseCase getVendorDashboardUseCase;

    @Mock private VendorInternalApi vendorInternalApi;

    @InjectMocks private VendorDashboardController vendorDashboardController;

    private final UUID vendorUserId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(vendorDashboardController)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();

        vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setUserId(vendorUserId);
        vendor.setBusinessName("Dana Water Sports");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsVendor() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        vendorUserId.toString(),
                        "credentials",
                        List.of(new SimpleGrantedAuthority("ROLE_VENDOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/vendor/dashboard trả về 200 OK với số liệu dashboard của chính vendor")
    void getVendorDashboard_Success() throws Exception {
        authenticateAsVendor();

        when(vendorInternalApi.findByUserId(vendorUserId)).thenReturn(Optional.of(vendor));

        VendorDiagnosisAlert alert =
                new VendorDiagnosisAlert(
                        vendorId,
                        "Dana Water Sports",
                        VendorIssueType.OVERLOADED,
                        VendorAlertSeverity.CRITICAL,
                        "Các khung giờ dịch vụ đang quá tải: 96.00%",
                        96.0);

        DashboardMetrics mockMetrics =
                new DashboardMetrics(
                        BigDecimal.valueOf(25_000_000),
                        BigDecimal.valueOf(22_000_000),
                        80L,
                        75L,
                        5L,
                        93.75,
                        6.25,
                        List.of(alert));

        when(getVendorDashboardUseCase.execute(eq(vendorId), any(TimeRange.class)))
                .thenReturn(mockMetrics);

        mockMvc.perform(get("/api/vendor/dashboard").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(25000000))
                .andExpect(jsonPath("$.netPayout").value(22000000))
                .andExpect(jsonPath("$.newOrders").value(80))
                .andExpect(jsonPath("$.completedOrders").value(75))
                .andExpect(jsonPath("$.completionRate").value(93.75))
                .andExpect(jsonPath("$.cancellationRate").value(6.25))
                .andExpect(jsonPath("$.alerts", hasSize(1)))
                .andExpect(jsonPath("$.alerts[0].vendorId").value(vendorId.toString()))
                .andExpect(jsonPath("$.alerts[0].issueType").value("OVERLOADED"))
                .andExpect(jsonPath("$.alerts[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.alerts[0].metricValue").value(96.0));

        verify(getVendorDashboardUseCase).execute(eq(vendorId), any(TimeRange.class));
    }

    @Test
    @DisplayName(
            "GET /api/vendor/dashboard trả về 403 Forbidden khi không tìm thấy hồ sơ Vendor của"
                    + " user")
    void getVendorDashboard_VendorProfileNotFound_Returns403() throws Exception {
        authenticateAsVendor();

        when(vendorInternalApi.findByUserId(vendorUserId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/vendor/dashboard").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("GET /api/vendor/dashboard trả về 403 Forbidden khi người dùng chưa đăng nhập")
    void getVendorDashboard_Unauthenticated_Returns403() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/vendor/dashboard").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("GET /api/vendor/dashboard với khoảng thời gian from và to tùy chỉnh")
    void getVendorDashboard_WithCustomDateRange_Success() throws Exception {
        authenticateAsVendor();

        when(vendorInternalApi.findByUserId(vendorUserId)).thenReturn(Optional.of(vendor));

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 8);
        TimeRange expectedRange = TimeRange.of(from, to);

        when(getVendorDashboardUseCase.execute(eq(vendorId), eq(expectedRange)))
                .thenReturn(DashboardMetrics.empty());

        mockMvc.perform(
                        get("/api/vendor/dashboard")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-08")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(0))
                .andExpect(jsonPath("$.newOrders").value(0));

        verify(getVendorDashboardUseCase).execute(eq(vendorId), eq(expectedRange));
    }

    @Test
    @DisplayName("GET /api/vendor/dashboard với from sau to trả về 400 Bad Request")
    void getVendorDashboard_InvalidDateRange_ReturnsBadRequest() throws Exception {
        authenticateAsVendor();
        when(vendorInternalApi.findByUserId(vendorUserId)).thenReturn(Optional.of(vendor));

        mockMvc.perform(
                        get("/api/vendor/dashboard")
                                .param("from", "2026-10-25")
                                .param("to", "2026-10-10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
