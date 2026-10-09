package com.danasea.backend.modules.report.presentation;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.danasea.backend.modules.report.domain.enums.VendorAlertSeverity;
import com.danasea.backend.modules.report.domain.enums.VendorIssueType;
import com.danasea.backend.modules.report.domain.models.DashboardMetrics;
import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorPerformanceItem;
import com.danasea.backend.modules.report.infrastructure.csv.Rfc4180Utf8BomCsvExporter;
import com.danasea.backend.modules.report.presentation.controllers.AdminReportController;
import com.danasea.backend.modules.report.presentation.controllers.VendorDashboardController;
import com.danasea.backend.modules.report.presentation.controllers.VendorReportController;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.security.authorization.presentation.AdminController;
import com.danasea.backend.shared.presentation.GlobalExceptionHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Thử thách đối kháng độc lập (Adversarial Challenge Suite) cho Milestone 3.
 *
 * <p>Tập trung xác thực 4 chiều đối kháng quan trọng: 1. Vendor Isolation & Chống tấn công IDOR
 * trên mọi endpoint của Vendor. 2. Điều kiện biên ngày tháng & Validation tham số (from > to,
 * groupBy sai, type sai, from == to, năm nhuận). 3. Chuẩn hóa xuất CSV RFC 4180 và 3 byte UTF-8 BOM
 * (0xEF, 0xBB, 0xBF). 4. Tính toán toàn vẹn và độ tin cậy của mã phản hồi HTTP.
 */
@ExtendWith(MockitoExtension.class)
class AdversarialReportChallengeTest {

    @Mock private GetRevenueReportUseCase getRevenueReportUseCase;

    @Mock private GetBookingReportUseCase getBookingReportUseCase;

    @Mock private GetVendorPerformanceReportUseCase getVendorPerformanceReportUseCase;

    @Mock private ExportReportCsvUseCase exportReportCsvUseCase;

    @Mock private GetAdminDashboardUseCase getAdminDashboardUseCase;

    @Mock private GetVendorDashboardUseCase getVendorDashboardUseCase;

    @Mock private VendorInternalApi vendorInternalApi;

    @InjectMocks private VendorReportController vendorReportController;

    @InjectMocks private VendorDashboardController vendorDashboardController;

    @InjectMocks private AdminReportController adminReportController;

    @InjectMocks private AdminController adminController;

    private MockMvc vendorReportMockMvc;
    private MockMvc vendorDashboardMockMvc;
    private MockMvc adminReportMockMvc;
    private MockMvc adminDashboardMockMvc;

    private final UUID authenticatedUserId =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID legitimateVendorId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID attackerSpoofedVendorId =
            UUID.fromString("99999999-9999-9999-9999-999999999999");
    private Vendor legitimateVendor;

    @BeforeEach
    void setUp() {
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

        vendorReportMockMvc =
                MockMvcBuilders.standaloneSetup(vendorReportController)
                        .setControllerAdvice(exceptionHandler)
                        .build();

        vendorDashboardMockMvc =
                MockMvcBuilders.standaloneSetup(vendorDashboardController)
                        .setControllerAdvice(exceptionHandler)
                        .build();

        adminReportMockMvc =
                MockMvcBuilders.standaloneSetup(adminReportController)
                        .setControllerAdvice(exceptionHandler)
                        .build();

        adminDashboardMockMvc =
                MockMvcBuilders.standaloneSetup(adminController)
                        .setControllerAdvice(exceptionHandler)
                        .build();

        legitimateVendor = new Vendor();
        legitimateVendor.setId(legitimateVendorId);
        legitimateVendor.setUserId(authenticatedUserId);
        legitimateVendor.setBusinessName("Legitimate Vendor Ltd");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsLegitimateVendor() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUserId.toString(),
                        "credentials",
                        List.of(new SimpleGrantedAuthority("ROLE_VENDOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================================================
    // PHẦN 1: THỬ THÁCH ĐỐI KHÁNG VENDOR ISOLATION & IDOR IMMUNITY
    // =========================================================================
    @Nested
    @DisplayName("Thử thách 1: Vendor Isolation & Chống IDOR")
    class VendorIsolationAndIdorTests {

        @Test
        @DisplayName(
                "Đối kháng IDOR /revenue: Kẻ tấn công truyền vendorId giả mạo -> Controller chỉ lấy"
                        + " Vendor đăng nhập")
        void revenueEndpoint_RejectsSpoofedVendorId_UsesAuthenticatedVendorOnly() throws Exception {
            authenticateAsLegitimateVendor();
            when(vendorInternalApi.findByUserId(authenticatedUserId))
                    .thenReturn(Optional.of(legitimateVendor));

            LocalDate from = LocalDate.of(2026, 10, 1);
            LocalDate to = LocalDate.of(2026, 10, 10);
            TimeRange range = TimeRange.of(from, to);

            when(getRevenueReportUseCase.execute(
                            eq(range), eq(GroupByPeriod.DAY), eq(legitimateVendorId)))
                    .thenReturn(Collections.emptyList());

            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/revenue")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10")
                                    .param("vendorId", attackerSpoofedVendorId.toString()))
                    .andExpect(status().isOk());

            // KHẲNG ĐỊNH: UseCase CHỈ nhận legitimateVendorId, TUYỆT ĐỐI KHÔNG nhận
            // attackerSpoofedVendorId
            verify(getRevenueReportUseCase)
                    .execute(eq(range), eq(GroupByPeriod.DAY), eq(legitimateVendorId));
            verify(getRevenueReportUseCase, never())
                    .execute(any(), any(), eq(attackerSpoofedVendorId));
        }

        @Test
        @DisplayName(
                "Đối kháng IDOR /bookings: Kẻ tấn công truyền vendorId giả mạo -> Controller chỉ"
                        + " lấy Vendor đăng nhập")
        void bookingsEndpoint_RejectsSpoofedVendorId_UsesAuthenticatedVendorOnly()
                throws Exception {
            authenticateAsLegitimateVendor();
            when(vendorInternalApi.findByUserId(authenticatedUserId))
                    .thenReturn(Optional.of(legitimateVendor));

            LocalDate from = LocalDate.of(2026, 10, 1);
            LocalDate to = LocalDate.of(2026, 10, 10);
            TimeRange range = TimeRange.of(from, to);

            when(getBookingReportUseCase.execute(
                            eq(range), eq(GroupByPeriod.DAY), eq(legitimateVendorId)))
                    .thenReturn(Collections.emptyList());

            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/bookings")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10")
                                    .param("vendorId", attackerSpoofedVendorId.toString()))
                    .andExpect(status().isOk());

            verify(getBookingReportUseCase)
                    .execute(eq(range), eq(GroupByPeriod.DAY), eq(legitimateVendorId));
            verify(getBookingReportUseCase, never())
                    .execute(any(), any(), eq(attackerSpoofedVendorId));
        }

        @Test
        @DisplayName(
                "Đối kháng IDOR /export: Kẻ tấn công truyền vendorId giả mạo -> Controller chỉ lấy"
                        + " Vendor đăng nhập")
        void exportEndpoint_RejectsSpoofedVendorId_UsesAuthenticatedVendorOnly() throws Exception {
            authenticateAsLegitimateVendor();
            when(vendorInternalApi.findByUserId(authenticatedUserId))
                    .thenReturn(Optional.of(legitimateVendor));

            LocalDate from = LocalDate.of(2026, 10, 1);
            LocalDate to = LocalDate.of(2026, 10, 10);
            TimeRange range = TimeRange.of(from, to);

            byte[] csvBytes = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'O', 'K'};
            when(exportReportCsvUseCase.execute(
                            eq(ReportType.REVENUE),
                            eq(range),
                            eq(GroupByPeriod.DAY),
                            eq(legitimateVendorId)))
                    .thenReturn(csvBytes);

            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/export")
                                    .param("type", "revenue")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10")
                                    .param("vendorId", attackerSpoofedVendorId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(
                            header().string(
                                            HttpHeaders.CONTENT_TYPE,
                                            is("text/csv;charset=UTF-8")));

            verify(exportReportCsvUseCase)
                    .execute(
                            eq(ReportType.REVENUE),
                            eq(range),
                            eq(GroupByPeriod.DAY),
                            eq(legitimateVendorId));
            verify(exportReportCsvUseCase, never())
                    .execute(any(), any(), any(), eq(attackerSpoofedVendorId));
        }

        @Test
        @DisplayName(
                "Đối kháng IDOR /dashboard: Kẻ tấn công truyền vendorId giả mạo -> Controller chỉ"
                        + " lấy Vendor đăng nhập")
        void vendorDashboard_RejectsSpoofedVendorId_UsesAuthenticatedVendorOnly() throws Exception {
            authenticateAsLegitimateVendor();
            when(vendorInternalApi.findByUserId(authenticatedUserId))
                    .thenReturn(Optional.of(legitimateVendor));

            when(getVendorDashboardUseCase.execute(eq(legitimateVendorId), any(TimeRange.class)))
                    .thenReturn(DashboardMetrics.empty());

            vendorDashboardMockMvc
                    .perform(
                            get("/api/vendor/dashboard")
                                    .param("vendorId", attackerSpoofedVendorId.toString()))
                    .andExpect(status().isOk());

            verify(getVendorDashboardUseCase).execute(eq(legitimateVendorId), any(TimeRange.class));
            verify(getVendorDashboardUseCase, never())
                    .execute(eq(attackerSpoofedVendorId), any(TimeRange.class));
        }

        @Test
        @DisplayName(
                "Thử gọi tất cả 4 Vendor endpoints khi user chưa có hồ sơ Vendor -> Phải nhận HTTP"
                        + " 403 Forbidden")
        void allVendorEndpoints_WhenVendorProfileNotFound_Returns403Forbidden() throws Exception {
            authenticateAsLegitimateVendor();
            when(vendorInternalApi.findByUserId(authenticatedUserId)).thenReturn(Optional.empty());

            // 1. /api/vendor/dashboard
            vendorDashboardMockMvc
                    .perform(get("/api/vendor/dashboard"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

            // 2. /api/vendor/reports/revenue
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/revenue")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

            // 3. /api/vendor/reports/bookings
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/bookings")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

            // 4. /api/vendor/reports/export
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/export")
                                    .param("type", "revenue")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName(
                "Thử gọi tất cả 4 Vendor endpoints khi chưa đăng nhập (SecurityContext rỗng) ->"
                        + " Phải nhận HTTP 403 Forbidden")
        void allVendorEndpoints_WhenUnauthenticated_Returns403Forbidden() throws Exception {
            SecurityContextHolder.clearContext();

            // 1. /api/vendor/dashboard
            vendorDashboardMockMvc
                    .perform(get("/api/vendor/dashboard"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

            // 2. /api/vendor/reports/revenue
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/revenue")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

            // 3. /api/vendor/reports/bookings
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/bookings")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

            // 4. /api/vendor/reports/export
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/export")
                                    .param("type", "revenue")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
    }

    // =========================================================================
    // PHẦN 2: THỬ THÁCH ĐỐI KHÁNG BOUNDARY CONDITIONS & VALIDATION
    // =========================================================================
    @Nested
    @DisplayName("Thử thách 2: Điều kiện biên & Tham số bất thường")
    class BoundaryConditionAndValidationTests {

        @Test
        @DisplayName(
                "Boundary from > to: Thử trên TOÀN BỘ 9 endpoints báo cáo -> Đều phải ném HTTP 400"
                        + " Bad Request")
        void allEndpoints_WithFromAfterTo_Return400BadRequest() throws Exception {
            authenticateAsLegitimateVendor();
            when(vendorInternalApi.findByUserId(authenticatedUserId))
                    .thenReturn(Optional.of(legitimateVendor));

            String invalidFrom = "2026-12-31";
            String invalidTo = "2026-01-01";

            // 1. Admin Dashboard
            adminDashboardMockMvc
                    .perform(
                            get("/api/admin/dashboard")
                                    .param("from", invalidFrom)
                                    .param("to", invalidTo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // 2. Admin Revenue Report
            adminReportMockMvc
                    .perform(
                            get("/api/admin/reports/revenue")
                                    .param("from", invalidFrom)
                                    .param("to", invalidTo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // 3. Admin Booking Report
            adminReportMockMvc
                    .perform(
                            get("/api/admin/reports/bookings")
                                    .param("from", invalidFrom)
                                    .param("to", invalidTo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // 4. Admin Vendor Performance Report
            adminReportMockMvc
                    .perform(
                            get("/api/admin/reports/vendors")
                                    .param("from", invalidFrom)
                                    .param("to", invalidTo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // 5. Admin Export Report
            adminReportMockMvc
                    .perform(
                            get("/api/admin/reports/export")
                                    .param("type", "revenue")
                                    .param("from", invalidFrom)
                                    .param("to", invalidTo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // 6. Vendor Dashboard
            vendorDashboardMockMvc
                    .perform(
                            get("/api/vendor/dashboard")
                                    .param("from", invalidFrom)
                                    .param("to", invalidTo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // 7. Vendor Revenue Report
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/revenue")
                                    .param("from", invalidFrom)
                                    .param("to", invalidTo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // 8. Vendor Booking Report
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/bookings")
                                    .param("from", invalidFrom)
                                    .param("to", invalidTo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // 9. Vendor Export Report
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/export")
                                    .param("type", "revenue")
                                    .param("from", invalidFrom)
                                    .param("to", invalidTo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"century", "millennium", "invalid_group", "123", "!@#$"})
        @DisplayName(
                "Boundary groupBy không hợp lệ: Thử nhiều giá trị độc hại -> Trả về HTTP 400 Bad"
                        + " Request")
        void invalidGroupBy_AcrossEndpoints_Returns400BadRequest(String invalidGroupBy)
                throws Exception {
            authenticateAsLegitimateVendor();
            when(vendorInternalApi.findByUserId(authenticatedUserId))
                    .thenReturn(Optional.of(legitimateVendor));

            // Admin Revenue
            adminReportMockMvc
                    .perform(
                            get("/api/admin/reports/revenue")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10")
                                    .param("groupBy", invalidGroupBy))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // Admin Bookings
            adminReportMockMvc
                    .perform(
                            get("/api/admin/reports/bookings")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10")
                                    .param("groupBy", invalidGroupBy))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // Admin Export
            adminReportMockMvc
                    .perform(
                            get("/api/admin/reports/export")
                                    .param("type", "revenue")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10")
                                    .param("groupBy", invalidGroupBy))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // Vendor Revenue
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/revenue")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10")
                                    .param("groupBy", invalidGroupBy))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // Vendor Bookings
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/bookings")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10")
                                    .param("groupBy", invalidGroupBy))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // Vendor Export
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/export")
                                    .param("type", "revenue")
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10")
                                    .param("groupBy", invalidGroupBy))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @ParameterizedTest
        @ValueSource(
                strings = {"sql_injection", "SYSTEM", "customers", "TRANSACTIONS", "undefined", ""})
        @DisplayName("Boundary type trong /export không hợp lệ -> Trả về HTTP 400 Bad Request")
        void invalidReportType_AcrossExportEndpoints_Returns400BadRequest(String invalidType)
                throws Exception {
            authenticateAsLegitimateVendor();
            when(vendorInternalApi.findByUserId(authenticatedUserId))
                    .thenReturn(Optional.of(legitimateVendor));

            // Admin Export
            adminReportMockMvc
                    .perform(
                            get("/api/admin/reports/export")
                                    .param("type", invalidType)
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

            // Vendor Export
            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/export")
                                    .param("type", invalidType)
                                    .param("from", "2026-10-01")
                                    .param("to", "2026-10-10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName(
                "Boundary from == to (khoảng thời gian 1 ngày duy nhất) -> Hoạt động chính xác 200"
                        + " OK")
        void singleDayRange_FromEqualsTo_OperatesCorrectly() throws Exception {
            authenticateAsLegitimateVendor();
            when(vendorInternalApi.findByUserId(authenticatedUserId))
                    .thenReturn(Optional.of(legitimateVendor));

            LocalDate singleDay = LocalDate.of(2026, 10, 15);
            TimeRange range = TimeRange.of(singleDay, singleDay);
            assertThat(range.getDaysCount()).isEqualTo(1L);

            when(getRevenueReportUseCase.execute(
                            eq(range), eq(GroupByPeriod.DAY), eq(legitimateVendorId)))
                    .thenReturn(Collections.emptyList());

            vendorReportMockMvc
                    .perform(
                            get("/api/vendor/reports/revenue")
                                    .param("from", "2026-10-15")
                                    .param("to", "2026-10-15"))
                    .andExpect(status().isOk());

            verify(getRevenueReportUseCase)
                    .execute(eq(range), eq(GroupByPeriod.DAY), eq(legitimateVendorId));
        }

        @Test
        @DisplayName(
                "Boundary Năm nhuận: ngày 29 tháng 2 (2024-02-29) -> Xử lý hợp lệ không gây lỗi")
        void leapYearRange_HandledProperly() throws Exception {
            LocalDate from = LocalDate.of(2024, 2, 28);
            LocalDate to = LocalDate.of(2024, 2, 29);
            TimeRange range = TimeRange.of(from, to);
            assertThat(range.getDaysCount()).isEqualTo(2L);

            when(getRevenueReportUseCase.execute(eq(range), eq(GroupByPeriod.DAY), isNull()))
                    .thenReturn(Collections.emptyList());

            adminReportMockMvc
                    .perform(
                            get("/api/admin/reports/revenue")
                                    .param("from", "2024-02-28")
                                    .param("to", "2024-02-29"))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // PHẦN 3: THỬ THÁCH ĐỐI KHÁNG ĐỊNH DẠNG XUẤT CSV (RFC 4180 & UTF-8 BOM)
    // =========================================================================
    @Nested
    @DisplayName("Thử thách 3: Định dạng xuất CSV & RFC 4180")
    class CsvFormatAndRfc4180Tests {

        private final Rfc4180Utf8BomCsvExporter exporter = new Rfc4180Utf8BomCsvExporter();
        private final TimeRange sampleRange =
                TimeRange.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        @Test
        @DisplayName(
                "Byte-level check: 3 byte đầu của mọi file CSV xuất ra luôn luôn là UTF-8 BOM"
                        + " (0xEF, 0xBB, 0xBF)")
        void byteLevelCheck_PrefixIsExactlyThreeByteUtf8Bom() {
            byte[] revenueBytes =
                    exporter.exportRevenueReport(Collections.emptyList(), sampleRange);
            byte[] bookingBytes =
                    exporter.exportBookingReport(Collections.emptyList(), sampleRange);
            byte[] vendorBytes =
                    exporter.exportVendorPerformanceReport(Collections.emptyList(), sampleRange);

            byte[] expectedBom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

            assertThat(revenueBytes).startsWith(expectedBom);
            assertThat(bookingBytes).startsWith(expectedBom);
            assertThat(vendorBytes).startsWith(expectedBom);
        }

        @Test
        @DisplayName(
                "RFC 4180 check: Dòng kết thúc bằng CRLF (\\r\\n), không dùng LF thuần túy (\\n)")
        void lineEndingCheck_EveryLineEndsWithCRLF() {
            RevenueReportItem item =
                    new RevenueReportItem(
                            "2026-10-01",
                            BigDecimal.valueOf(10_000_000),
                            BigDecimal.ZERO,
                            BigDecimal.valueOf(10_000_000),
                            BigDecimal.ZERO,
                            BigDecimal.valueOf(1_000_000),
                            BigDecimal.valueOf(9_000_000),
                            5L);

            byte[] csvBytes = exporter.exportRevenueReport(List.of(item), sampleRange);
            String rawCsv = new String(csvBytes, 3, csvBytes.length - 3, StandardCharsets.UTF_8);

            // Kiểm tra CRLF xuất hiện
            assertThat(rawCsv).contains("\r\n");

            // Kiểm tra tất cả các dòng đều kết thúc đúng bằng \r\n (không có \n lẻ loi đứng trước
            // không có \r)
            String[] lines = rawCsv.split("\r\n");
            assertThat(lines.length)
                    .isGreaterThanOrEqualTo(2); // Ít nhất 1 dòng header và 1 dòng dữ liệu
            for (String line : lines) {
                assertThat(line).doesNotContain("\n");
                assertThat(line).doesNotContain("\r");
            }
        }

        @Test
        @DisplayName(
                "RFC 4180 check: Thoát chuỗi (escaping) chuẩn xác cho dấu phẩy, nháy kép, và ngắt"
                        + " dòng")
        void rfc4180Escaping_HandlesQuotesCommasAndNewlines() {
            // Trường có dấu phẩy -> bọc trong dấu nháy kép
            assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Đà Nẵng, Việt Nam"))
                    .isEqualTo("\"Đà Nẵng, Việt Nam\"");

            // Trường có dấu nháy kép -> bọc trong nháy kép và nhân đôi dấu nháy kép (" -> "")
            assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Dịch vụ \"Ca Nô Cao Tốc\""))
                    .isEqualTo("\"Dịch vụ \"\"Ca Nô Cao Tốc\"\"\"");

            // Trường có ký tự xuống dòng (\n hoặc \r) -> bọc trong dấu nháy kép
            assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Dòng 1\nDòng 2"))
                    .isEqualTo("\"Dòng 1\nDòng 2\"");
            assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Dòng 1\r\nDòng 2"))
                    .isEqualTo("\"Dòng 1\r\nDòng 2\"");

            // Trường kết hợp cả nháy kép và dấu phẩy
            assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Tour \"VIP\", Đảo Cù Lao Chàm"))
                    .isEqualTo("\"Tour \"\"VIP\"\", Đảo Cù Lao Chàm\"");

            // Trường thông thường không chứa ký tự đặc biệt -> giữ nguyên
            assertThat(Rfc4180Utf8BomCsvExporter.escapeField("DanaseaReports"))
                    .isEqualTo("DanaseaReports");

            // Trường null -> trả về rỗng
            assertThat(Rfc4180Utf8BomCsvExporter.escapeField(null)).isEqualTo("");
        }

        @Test
        @DisplayName(
                "Tiếng Việt có dấu: UTF-8 BOM bảo đảm hiển thị tiếng Việt vẹn toàn, không lỗi font")
        void vietnameseCharacters_DecodeFlawlesslyWithoutEncodingCorruption() {
            VendorPerformanceItem item =
                    new VendorPerformanceItem(
                            legitimateVendorId,
                            "Công Ty Cổ Phần Ca Nô Biển Đà Nẵng",
                            BigDecimal.valueOf(100_000_000),
                            BigDecimal.valueOf(90_000_000),
                            50L,
                            45L,
                            5L,
                            10.0,
                            88.0,
                            BigDecimal.valueOf(4.8),
                            20,
                            VendorAlertSeverity.HEALTHY,
                            List.of(VendorIssueType.HIGH_CANCELLATION));

            byte[] csvBytes = exporter.exportVendorPerformanceReport(List.of(item), sampleRange);
            String content = new String(csvBytes, 3, csvBytes.length - 3, StandardCharsets.UTF_8);

            assertThat(content).contains("Mã đối tác");
            assertThat(content).contains("Tên đối tác");
            assertThat(content).contains("Tổng doanh thu");
            assertThat(content).contains("Công Ty Cổ Phần Ca Nô Biển Đà Nẵng");
            assertThat(content).contains("Đánh giá trung bình");
            assertThat(content).contains("Mức độ cảnh báo");
        }

        @Test
        @DisplayName(
                "Headers HTTP: Kiểm tra Content-Type text/csv;charset=UTF-8 và Content-Disposition"
                        + " chuẩn")
        void httpHeaders_MeetStrictExpectations() throws Exception {
            LocalDate from = LocalDate.of(2026, 10, 1);
            LocalDate to = LocalDate.of(2026, 10, 15);
            TimeRange range = TimeRange.of(from, to);

            byte[] dummyCsv = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'C', 'S', 'V'};
            when(exportReportCsvUseCase.execute(
                            eq(ReportType.BOOKINGS), eq(range), eq(GroupByPeriod.DAY), isNull()))
                    .thenReturn(dummyCsv);

            MvcResult result =
                    adminReportMockMvc
                            .perform(
                                    get("/api/admin/reports/export")
                                            .param("type", "bookings")
                                            .param("from", "2026-10-01")
                                            .param("to", "2026-10-15"))
                            .andExpect(status().isOk())
                            .andExpect(
                                    header().string(
                                                    HttpHeaders.CONTENT_TYPE,
                                                    is("text/csv;charset=UTF-8")))
                            .andExpect(
                                    header().string(
                                                    HttpHeaders.CONTENT_DISPOSITION,
                                                    is(
                                                            "attachment;"
                                                                + " filename=\"report-bookings-2026-10-01-2026-10-15.csv\"")))
                            .andReturn();

            byte[] content = result.getResponse().getContentAsByteArray();
            assertThat(content[0]).isEqualTo((byte) 0xEF);
            assertThat(content[1]).isEqualTo((byte) 0xBB);
            assertThat(content[2]).isEqualTo((byte) 0xBF);
        }
    }
}
