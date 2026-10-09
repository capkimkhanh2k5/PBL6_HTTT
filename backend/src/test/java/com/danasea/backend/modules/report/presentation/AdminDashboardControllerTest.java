package com.danasea.backend.modules.report.presentation;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.danasea.backend.modules.report.application.usecases.GetAdminDashboardUseCase;
import com.danasea.backend.modules.report.domain.enums.VendorAlertSeverity;
import com.danasea.backend.modules.report.domain.enums.VendorIssueType;
import com.danasea.backend.modules.report.domain.models.DashboardMetrics;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorDiagnosisAlert;
import com.danasea.backend.security.authorization.presentation.AdminController;
import com.danasea.backend.shared.presentation.GlobalExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock private GetAdminDashboardUseCase getAdminDashboardUseCase;

    @InjectMocks private AdminController adminController;

    private final UUID sampleVendorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(adminController)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    @DisplayName(
            "GET /api/admin/dashboard mặc định trả về 200 OK với status ADMIN_ACCESS_GRANTED và số"
                    + " liệu thực")
    void getAdminDashboard_Default_ReturnsOkWithRealMetrics() throws Exception {
        VendorDiagnosisAlert alert =
                new VendorDiagnosisAlert(
                        sampleVendorId,
                        "Dana Canoeing",
                        VendorIssueType.HIGH_CANCELLATION,
                        VendorAlertSeverity.WARNING,
                        "Tỷ lệ hủy đơn cao: 22.50%",
                        22.5);

        DashboardMetrics mockMetrics =
                new DashboardMetrics(
                        BigDecimal.valueOf(15_000_000),
                        BigDecimal.valueOf(1_500_000),
                        50L,
                        40L,
                        10L,
                        80.0,
                        20.0,
                        List.of(alert));

        when(getAdminDashboardUseCase.execute(any(TimeRange.class), isNull()))
                .thenReturn(mockMetrics);

        mockMvc.perform(get("/api/admin/dashboard").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADMIN_ACCESS_GRANTED"))
                .andExpect(jsonPath("$.totalRevenue").value(15000000))
                .andExpect(jsonPath("$.netPayout").value(1500000))
                .andExpect(jsonPath("$.newOrders").value(50))
                .andExpect(jsonPath("$.completedOrders").value(40))
                .andExpect(jsonPath("$.completionRate").value(80.0))
                .andExpect(jsonPath("$.cancellationRate").value(20.0))
                .andExpect(jsonPath("$.alerts", hasSize(1)))
                .andExpect(jsonPath("$.alerts[0].vendorId").value(sampleVendorId.toString()))
                .andExpect(jsonPath("$.alerts[0].businessName").value("Dana Canoeing"))
                .andExpect(jsonPath("$.alerts[0].issueType").value("HIGH_CANCELLATION"))
                .andExpect(jsonPath("$.alerts[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.alerts[0].message").value("Tỷ lệ hủy đơn cao: 22.50%"))
                .andExpect(jsonPath("$.alerts[0].metricValue").value(22.5));

        verify(getAdminDashboardUseCase).execute(any(TimeRange.class), isNull());
    }

    @Test
    @DisplayName("GET /api/admin/dashboard với from, to và vendorId lọc chính xác")
    void getAdminDashboard_WithDateFilterAndVendorId_ReturnsFilteredMetrics() throws Exception {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 15);
        TimeRange expectedRange = TimeRange.of(from, to);

        DashboardMetrics mockMetrics =
                new DashboardMetrics(
                        BigDecimal.valueOf(5_000_000),
                        BigDecimal.valueOf(4_500_000),
                        20L,
                        18L,
                        2L,
                        90.0,
                        10.0,
                        List.of());

        when(getAdminDashboardUseCase.execute(eq(expectedRange), eq(sampleVendorId)))
                .thenReturn(mockMetrics);

        mockMvc.perform(
                        get("/api/admin/dashboard")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-15")
                                .param("vendorId", sampleVendorId.toString())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADMIN_ACCESS_GRANTED"))
                .andExpect(jsonPath("$.totalRevenue").value(5000000))
                .andExpect(jsonPath("$.netPayout").value(4500000))
                .andExpect(jsonPath("$.newOrders").value(20))
                .andExpect(jsonPath("$.completedOrders").value(18))
                .andExpect(jsonPath("$.completionRate").value(90.0))
                .andExpect(jsonPath("$.cancellationRate").value(10.0))
                .andExpect(jsonPath("$.alerts", hasSize(0)));

        verify(getAdminDashboardUseCase).execute(eq(expectedRange), eq(sampleVendorId));
    }

    @Test
    @DisplayName("GET /api/admin/dashboard với from sau to trả về 400 Bad Request")
    void getAdminDashboard_InvalidDateRange_ReturnsBadRequest() throws Exception {
        mockMvc.perform(
                        get("/api/admin/dashboard")
                                .param("from", "2026-10-20")
                                .param("to", "2026-10-10")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/admin/dashboard khi không có dữ liệu trả về metrics rỗng sạch sẽ")
    void getAdminDashboard_EmptyMetrics_ReturnsZeroValues() throws Exception {
        when(getAdminDashboardUseCase.execute(any(TimeRange.class), isNull()))
                .thenReturn(DashboardMetrics.empty());

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADMIN_ACCESS_GRANTED"))
                .andExpect(jsonPath("$.totalRevenue").value(0))
                .andExpect(jsonPath("$.netPayout").value(0))
                .andExpect(jsonPath("$.newOrders").value(0))
                .andExpect(jsonPath("$.completedOrders").value(0))
                .andExpect(jsonPath("$.completionRate").value(0.0))
                .andExpect(jsonPath("$.cancellationRate").value(0.0))
                .andExpect(jsonPath("$.alerts", hasSize(0)));
    }
}
