package com.danasea.backend.modules.report.presentation;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
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
import com.danasea.backend.modules.report.application.usecases.GetVendorPerformanceReportUseCase;
import com.danasea.backend.modules.report.domain.enums.GroupByPeriod;
import com.danasea.backend.modules.report.domain.enums.ReportType;
import com.danasea.backend.modules.report.domain.enums.VendorAlertSeverity;
import com.danasea.backend.modules.report.domain.enums.VendorIssueType;
import com.danasea.backend.modules.report.domain.models.BookingReportItem;
import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorPerformanceItem;
import com.danasea.backend.modules.report.presentation.controllers.AdminReportController;
import com.danasea.backend.shared.presentation.GlobalExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AdminReportControllerTest {

    private MockMvc mockMvc;

    @Mock private GetRevenueReportUseCase getRevenueReportUseCase;

    @Mock private GetBookingReportUseCase getBookingReportUseCase;

    @Mock private GetVendorPerformanceReportUseCase getVendorPerformanceReportUseCase;

    @Mock private ExportReportCsvUseCase exportReportCsvUseCase;

    @InjectMocks private AdminReportController adminReportController;

    private final UUID sampleVendorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(adminReportController)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    @DisplayName(
            "GET /api/admin/reports/revenue trả về 200 OK với danh sách dữ liệu doanh thu và"
                    + " timeRange")
    void getRevenueReport_Success() throws Exception {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 2);
        TimeRange expectedRange = TimeRange.of(from, to);

        RevenueReportItem item1 =
                new RevenueReportItem(
                        "2026-10-01",
                        BigDecimal.valueOf(10_000_000),
                        BigDecimal.valueOf(500_000),
                        BigDecimal.valueOf(9_500_000),
                        BigDecimal.valueOf(1_000_000),
                        BigDecimal.valueOf(950_000),
                        BigDecimal.valueOf(7_550_000),
                        10L);
        RevenueReportItem item2 = RevenueReportItem.empty("2026-10-02");

        when(getRevenueReportUseCase.execute(
                        eq(expectedRange), eq(GroupByPeriod.DAY), eq(sampleVendorId)))
                .thenReturn(List.of(item1, item2));

        mockMvc.perform(
                        get("/api/admin/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-02")
                                .param("groupBy", "day")
                                .param("vendorId", sampleVendorId.toString())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeRange.from").value("2026-10-01"))
                .andExpect(jsonPath("$.timeRange.to").value("2026-10-02"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].periodKey").value("2026-10-01"))
                .andExpect(jsonPath("$.items[0].gmv").value(10000000))
                .andExpect(jsonPath("$.items[0].discountAmount").value(500000))
                .andExpect(jsonPath("$.items[0].collectedCash").value(9500000))
                .andExpect(jsonPath("$.items[0].refunds").value(1000000))
                .andExpect(jsonPath("$.items[0].platformCommission").value(950000))
                .andExpect(jsonPath("$.items[0].netVendorPayout").value(7550000))
                .andExpect(jsonPath("$.items[0].orderCount").value(10))
                .andExpect(jsonPath("$.items[1].periodKey").value("2026-10-02"))
                .andExpect(jsonPath("$.items[1].gmv").value(0));

        verify(getRevenueReportUseCase)
                .execute(eq(expectedRange), eq(GroupByPeriod.DAY), eq(sampleVendorId));
    }

    @Test
    @DisplayName("GET /api/admin/reports/bookings trả về 200 OK với phân loại lý do hủy đơn")
    void getBookingReport_Success() throws Exception {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 7);
        TimeRange expectedRange = TimeRange.of(from, to);

        BookingReportItem item =
                new BookingReportItem(
                        "2026-W40",
                        25L,
                        20L,
                        5L,
                        Map.of(RefundReason.WEATHER, 3L, RefundReason.CUSTOMER_CANCEL, 2L));

        when(getBookingReportUseCase.execute(eq(expectedRange), eq(GroupByPeriod.WEEK), eq(null)))
                .thenReturn(List.of(item));

        mockMvc.perform(
                        get("/api/admin/reports/bookings")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-07")
                                .param("groupBy", "week")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].periodKey").value("2026-W40"))
                .andExpect(jsonPath("$.items[0].totalOrders").value(25))
                .andExpect(jsonPath("$.items[0].completedOrders").value(20))
                .andExpect(jsonPath("$.items[0].cancelledOrders").value(5))
                .andExpect(jsonPath("$.items[0].cancellationReasonBreakdown.WEATHER").value(3))
                .andExpect(
                        jsonPath("$.items[0].cancellationReasonBreakdown.CUSTOMER_CANCEL")
                                .value(2));

        verify(getBookingReportUseCase)
                .execute(eq(expectedRange), eq(GroupByPeriod.WEEK), eq(null));
    }

    @Test
    @DisplayName(
            "GET /api/admin/reports/vendors trả về 200 OK với hiệu suất đối tác và chẩn đoán thông"
                    + " minh")
    void getVendorPerformanceReport_Success() throws Exception {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        TimeRange expectedRange = TimeRange.of(from, to);

        VendorPerformanceItem vendorItem =
                new VendorPerformanceItem(
                        sampleVendorId,
                        "Dana Canoeing",
                        BigDecimal.valueOf(50_000_000),
                        BigDecimal.valueOf(45_000_000),
                        100L,
                        80L,
                        20L,
                        20.0,
                        92.5,
                        BigDecimal.valueOf(4.5),
                        25,
                        VendorAlertSeverity.HEALTHY,
                        List.of(VendorIssueType.HIGH_CANCELLATION));

        when(getVendorPerformanceReportUseCase.execute(eq(expectedRange), eq(null)))
                .thenReturn(List.of(vendorItem));

        mockMvc.perform(
                        get("/api/admin/reports/vendors")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-31")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].vendorId").value(sampleVendorId.toString()))
                .andExpect(jsonPath("$.items[0].businessName").value("Dana Canoeing"))
                .andExpect(jsonPath("$.items[0].revenue").value(50000000))
                .andExpect(jsonPath("$.items[0].netPayout").value(45000000))
                .andExpect(jsonPath("$.items[0].totalOrders").value(100))
                .andExpect(jsonPath("$.items[0].completedOrders").value(80))
                .andExpect(jsonPath("$.items[0].cancelledOrders").value(20))
                .andExpect(jsonPath("$.items[0].slotOccupancyRate").value(92.5))
                .andExpect(jsonPath("$.items[0].rating").value(4.5))
                .andExpect(jsonPath("$.items[0].severity").value("HEALTHY"))
                .andExpect(jsonPath("$.items[0].issues[0]").value("HIGH_CANCELLATION"));

        verify(getVendorPerformanceReportUseCase).execute(eq(expectedRange), eq(null));
    }

    @Test
    @DisplayName("GET /api/admin/reports/export trả về 200 OK với file CSV và headers hợp lệ")
    void exportReport_Success() throws Exception {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        TimeRange expectedRange = TimeRange.of(from, to);

        byte[] fakeCsv =
                new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'H', 'e', 'a', 'd', 'e', 'r'};
        when(exportReportCsvUseCase.execute(
                        eq(ReportType.REVENUE), eq(expectedRange), eq(GroupByPeriod.DAY), eq(null)))
                .thenReturn(fakeCsv);

        mockMvc.perform(
                        get("/api/admin/reports/export")
                                .param("type", "revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, is("text/csv;charset=UTF-8")))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        is(
                                                "attachment;"
                                                    + " filename=\"report-revenue-2026-10-01-2026-10-31.csv\"")))
                .andExpect(content().bytes(fakeCsv));

        verify(exportReportCsvUseCase)
                .execute(
                        eq(ReportType.REVENUE), eq(expectedRange), eq(GroupByPeriod.DAY), eq(null));
    }

    @Test
    @DisplayName("GET /api/admin/reports/revenue với from sau to trả về 400 Bad Request")
    void getRevenueReport_InvalidDateRange_ReturnsBadRequest() throws Exception {
        mockMvc.perform(
                        get("/api/admin/reports/revenue")
                                .param("from", "2026-10-31")
                                .param("to", "2026-10-01")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/admin/reports/revenue với groupBy không hỗ trợ trả về 400 Bad Request")
    void getRevenueReport_InvalidGroupBy_ReturnsBadRequest() throws Exception {
        mockMvc.perform(
                        get("/api/admin/reports/revenue")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-10")
                                .param("groupBy", "decade")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/admin/reports/export với type không hỗ trợ trả về 400 Bad Request")
    void exportReport_InvalidType_ReturnsBadRequest() throws Exception {
        mockMvc.perform(
                        get("/api/admin/reports/export")
                                .param("type", "unknown")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/admin/reports/bookings với from sau to trả về 400 Bad Request")
    void getBookingReport_InvalidDateRange_ReturnsBadRequest() throws Exception {
        mockMvc.perform(
                        get("/api/admin/reports/bookings")
                                .param("from", "2026-05-10")
                                .param("to", "2026-05-01")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/admin/reports/bookings với groupBy không hỗ trợ trả về 400 Bad Request")
    void getBookingReport_InvalidGroupBy_ReturnsBadRequest() throws Exception {
        mockMvc.perform(
                        get("/api/admin/reports/bookings")
                                .param("from", "2026-05-01")
                                .param("to", "2026-05-10")
                                .param("groupBy", "invalid_group")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/admin/reports/vendors với from sau to trả về 400 Bad Request")
    void getVendorPerformanceReport_InvalidDateRange_ReturnsBadRequest() throws Exception {
        mockMvc.perform(
                        get("/api/admin/reports/vendors")
                                .param("from", "2026-05-10")
                                .param("to", "2026-05-01")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/admin/reports/export với from sau to trả về 400 Bad Request")
    void exportReport_InvalidDateRange_ReturnsBadRequest() throws Exception {
        mockMvc.perform(
                        get("/api/admin/reports/export")
                                .param("type", "revenue")
                                .param("from", "2026-05-10")
                                .param("to", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/admin/reports/export với groupBy không hỗ trợ trả về 400 Bad Request")
    void exportReport_InvalidGroupBy_ReturnsBadRequest() throws Exception {
        mockMvc.perform(
                        get("/api/admin/reports/export")
                                .param("type", "revenue")
                                .param("from", "2026-05-01")
                                .param("to", "2026-05-10")
                                .param("groupBy", "invalid_group"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
