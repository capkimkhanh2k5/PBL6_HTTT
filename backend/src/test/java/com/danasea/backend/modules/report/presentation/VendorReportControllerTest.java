package com.danasea.backend.modules.report.presentation;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.report.application.usecases.ExportReportCsvUseCase;
import com.danasea.backend.modules.report.application.usecases.GetBookingReportUseCase;
import com.danasea.backend.modules.report.application.usecases.GetRevenueReportUseCase;
import com.danasea.backend.modules.report.domain.enums.GroupByPeriod;
import com.danasea.backend.modules.report.domain.enums.ReportType;
import com.danasea.backend.modules.report.domain.models.BookingReportItem;
import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.presentation.controllers.VendorReportController;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class VendorReportControllerTest {

    private MockMvc mockMvc;

    @Mock private GetRevenueReportUseCase getRevenueReportUseCase;

    @Mock private GetBookingReportUseCase getBookingReportUseCase;

    @Mock private ExportReportCsvUseCase exportReportCsvUseCase;

    @Mock private VendorInternalApi vendorInternalApi;

    @InjectMocks private VendorReportController vendorReportController;

    private final UUID authenticatedUserId = UUID.randomUUID();
    private final UUID legitimateVendorId = UUID.randomUUID();
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(vendorReportController)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();

        vendor = new Vendor();
        vendor.setId(legitimateVendorId);
        vendor.setUserId(authenticatedUserId);
        vendor.setBusinessName("Legitimate Vendor");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateVendor() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUserId.toString(),
                        "credentials",
                        List.of(new SimpleGrantedAuthority("ROLE_VENDOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName(
            "GET /api/vendor/reports/revenue trả về 200 OK và kiểm chứng Vendor Isolation nghiêm"
                    + " ngặt")
    void getRevenueReport_StrictVendorIsolation() throws Exception {
        authenticateVendor();
        when(vendorInternalApi.findByUserId(authenticatedUserId)).thenReturn(Optional.of(vendor));

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 5);
        TimeRange expectedRange = TimeRange.of(from, to);

        RevenueReportItem item =
                new RevenueReportItem(
                        "2026-10-01",
                        BigDecimal.valueOf(8_000_000),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(8_000_000),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(800_000),
                        BigDecimal.valueOf(7_200_000),
                        5L);

        when(getRevenueReportUseCase.execute(
                        eq(expectedRange), eq(GroupByPeriod.DAY), eq(legitimateVendorId)))
                .thenReturn(List.of(item));

        UUID maliciousVendorId = UUID.randomUUID();

        // Cố tình truyền vendorId giả mạo trên query string
        mockMvc.perform(
                        get("/api/vendor/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-05")
                                .param("groupBy", "day")
                                .param("vendorId", maliciousVendorId.toString())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].netVendorPayout").value(7200000));

        // Xác nhận use case CHỈ nhận legitimateVendorId, tuyệt đối KHÔNG nhận maliciousVendorId
        verify(getRevenueReportUseCase)
                .execute(eq(expectedRange), eq(GroupByPeriod.DAY), eq(legitimateVendorId));
        verify(getRevenueReportUseCase, never())
                .execute(eq(expectedRange), eq(GroupByPeriod.DAY), eq(maliciousVendorId));
    }

    @Test
    @DisplayName("GET /api/vendor/reports/bookings trả về 200 OK cho vendor hợp lệ")
    void getBookingReport_Success() throws Exception {
        authenticateVendor();
        when(vendorInternalApi.findByUserId(authenticatedUserId)).thenReturn(Optional.of(vendor));

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 10);
        TimeRange expectedRange = TimeRange.of(from, to);

        BookingReportItem item =
                new BookingReportItem("2026-10-01", 10L, 8L, 2L, Map.of(RefundReason.WEATHER, 2L));

        when(getBookingReportUseCase.execute(
                        eq(expectedRange), eq(GroupByPeriod.DAY), eq(legitimateVendorId)))
                .thenReturn(List.of(item));

        mockMvc.perform(
                        get("/api/vendor/reports/bookings")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-10")
                                .param("groupBy", "day")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].completedOrders").value(8))
                .andExpect(jsonPath("$.items[0].cancellationReasonBreakdown.WEATHER").value(2));

        verify(getBookingReportUseCase)
                .execute(eq(expectedRange), eq(GroupByPeriod.DAY), eq(legitimateVendorId));
    }

    @Test
    @DisplayName("GET /api/vendor/reports/export xuất CSV thành công chỉ với dữ liệu của vendor đó")
    void exportReport_Success() throws Exception {
        authenticateVendor();
        when(vendorInternalApi.findByUserId(authenticatedUserId)).thenReturn(Optional.of(vendor));

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 15);
        TimeRange expectedRange = TimeRange.of(from, to);

        byte[] fakeCsv =
                new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'V', 'e', 'n', 'd', 'o', 'r'};
        when(exportReportCsvUseCase.execute(
                        eq(ReportType.REVENUE),
                        eq(expectedRange),
                        eq(GroupByPeriod.DAY),
                        eq(legitimateVendorId)))
                .thenReturn(fakeCsv);

        mockMvc.perform(
                        get("/api/vendor/reports/export")
                                .param("type", "revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-15")
                                .param("groupBy", "day"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, is("text/csv;charset=UTF-8")))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        is(
                                                "attachment;"
                                                    + " filename=\"report-revenue-2026-10-01-2026-10-15.csv\"")))
                .andExpect(content().bytes(fakeCsv));

        verify(exportReportCsvUseCase)
                .execute(
                        eq(ReportType.REVENUE),
                        eq(expectedRange),
                        eq(GroupByPeriod.DAY),
                        eq(legitimateVendorId));
    }

    @Test
    @DisplayName(
            "GET /api/vendor/reports/revenue trả về 403 Forbidden khi không tìm thấy hồ sơ Vendor")
    void getRevenueReport_VendorProfileNotFound_Returns403() throws Exception {
        authenticateVendor();
        when(vendorInternalApi.findByUserId(authenticatedUserId)).thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/vendor/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-15"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("GET /api/vendor/reports/revenue trả về 403 Forbidden khi chưa đăng nhập")
    void getRevenueReport_Unauthenticated_Returns403() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(
                        get("/api/vendor/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-15"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("GET /api/vendor/reports/revenue với from sau to trả về 400 Bad Request")
    void getRevenueReport_InvalidDateRange_ReturnsBadRequest() throws Exception {
        authenticateVendor();
        when(vendorInternalApi.findByUserId(authenticatedUserId)).thenReturn(Optional.of(vendor));

        mockMvc.perform(
                        get("/api/vendor/reports/revenue")
                                .param("from", "2026-10-30")
                                .param("to", "2026-10-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/vendor/reports/revenue với groupBy không hỗ trợ trả về 400 Bad Request")
    void getRevenueReport_InvalidGroupBy_ReturnsBadRequest() throws Exception {
        authenticateVendor();
        when(vendorInternalApi.findByUserId(authenticatedUserId)).thenReturn(Optional.of(vendor));

        mockMvc.perform(
                        get("/api/vendor/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-10")
                                .param("groupBy", "invalid_group"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/vendor/reports/bookings với from sau to trả về 400 Bad Request")
    void getBookingReport_InvalidDateRange_ReturnsBadRequest() throws Exception {
        authenticateVendor();
        when(vendorInternalApi.findByUserId(authenticatedUserId)).thenReturn(Optional.of(vendor));

        mockMvc.perform(
                        get("/api/vendor/reports/bookings")
                                .param("from", "2026-05-10")
                                .param("to", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/vendor/reports/bookings với groupBy không hỗ trợ trả về 400 Bad Request")
    void getBookingReport_InvalidGroupBy_ReturnsBadRequest() throws Exception {
        authenticateVendor();
        when(vendorInternalApi.findByUserId(authenticatedUserId)).thenReturn(Optional.of(vendor));

        mockMvc.perform(
                        get("/api/vendor/reports/bookings")
                                .param("from", "2026-05-01")
                                .param("to", "2026-05-10")
                                .param("groupBy", "invalid_group"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/vendor/reports/export với from sau to trả về 400 Bad Request")
    void exportReport_InvalidDateRange_ReturnsBadRequest() throws Exception {
        authenticateVendor();
        when(vendorInternalApi.findByUserId(authenticatedUserId)).thenReturn(Optional.of(vendor));

        mockMvc.perform(
                        get("/api/vendor/reports/export")
                                .param("type", "revenue")
                                .param("from", "2026-05-10")
                                .param("to", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/vendor/reports/export với groupBy không hỗ trợ trả về 400 Bad Request")
    void exportReport_InvalidGroupBy_ReturnsBadRequest() throws Exception {
        authenticateVendor();
        when(vendorInternalApi.findByUserId(authenticatedUserId)).thenReturn(Optional.of(vendor));

        mockMvc.perform(
                        get("/api/vendor/reports/export")
                                .param("type", "revenue")
                                .param("from", "2026-05-01")
                                .param("to", "2026-05-10")
                                .param("groupBy", "invalid_group"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/vendor/reports/export với type không hỗ trợ trả về 400 Bad Request")
    void exportReport_InvalidType_ReturnsBadRequest() throws Exception {
        authenticateVendor();
        when(vendorInternalApi.findByUserId(authenticatedUserId)).thenReturn(Optional.of(vendor));

        mockMvc.perform(
                        get("/api/vendor/reports/export")
                                .param("type", "unknown_type")
                                .param("from", "2026-05-01")
                                .param("to", "2026-05-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
