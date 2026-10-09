package com.danasea.backend.modules.report.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.danasea.backend.modules.report.application.ports.CsvExporterPort;
import com.danasea.backend.modules.report.domain.enums.GroupByPeriod;
import com.danasea.backend.modules.report.domain.enums.ReportType;
import com.danasea.backend.modules.report.domain.models.BookingReportItem;
import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorPerformanceItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportReportCsvUseCase Workflow & Routing Tests")
class ExportReportCsvUseCaseTest {

    @Mock private GetRevenueReportUseCase getRevenueReportUseCase;

    @Mock private GetBookingReportUseCase getBookingReportUseCase;

    @Mock private GetVendorPerformanceReportUseCase getVendorPerformanceReportUseCase;

    @Mock private CsvExporterPort csvExporterPort;

    private ExportReportCsvUseCase useCase;
    private TimeRange timeRange;

    @BeforeEach
    void setUp() {
        useCase =
                new ExportReportCsvUseCase(
                        getRevenueReportUseCase,
                        getBookingReportUseCase,
                        getVendorPerformanceReportUseCase,
                        csvExporterPort);
        timeRange = TimeRange.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7));
    }

    @Test
    @DisplayName("Điều phối xuất CSV cho loại REVENUE chính xác")
    void shouldRouteRevenueCsvExport() {
        UUID vendorId = UUID.randomUUID();
        List<RevenueReportItem> items = List.of(RevenueReportItem.empty("2026-10-01"));
        byte[] expectedBytes = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'R', 'E', 'V'};

        when(getRevenueReportUseCase.execute(eq(timeRange), eq(GroupByPeriod.DAY), eq(vendorId)))
                .thenReturn(items);
        when(csvExporterPort.exportRevenueReport(eq(items), eq(timeRange)))
                .thenReturn(expectedBytes);

        byte[] result = useCase.execute(ReportType.REVENUE, timeRange, GroupByPeriod.DAY, vendorId);

        assertThat(result).isEqualTo(expectedBytes);
        verify(getRevenueReportUseCase).execute(timeRange, GroupByPeriod.DAY, vendorId);
        verify(csvExporterPort).exportRevenueReport(items, timeRange);
    }

    @Test
    @DisplayName("Điều phối xuất CSV cho loại BOOKINGS chính xác")
    void shouldRouteBookingCsvExport() {
        List<BookingReportItem> items = List.of(BookingReportItem.empty("2026-10-01"));
        byte[] expectedBytes =
                new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'B', 'O', 'O', 'K'};

        when(getBookingReportUseCase.execute(eq(timeRange), eq(GroupByPeriod.WEEK), eq(null)))
                .thenReturn(items);
        when(csvExporterPort.exportBookingReport(eq(items), eq(timeRange)))
                .thenReturn(expectedBytes);

        byte[] result = useCase.execute(ReportType.BOOKINGS, timeRange, GroupByPeriod.WEEK, null);

        assertThat(result).isEqualTo(expectedBytes);
        verify(getBookingReportUseCase).execute(timeRange, GroupByPeriod.WEEK, null);
        verify(csvExporterPort).exportBookingReport(items, timeRange);
    }

    @Test
    @DisplayName("Điều phối xuất CSV cho loại VENDORS chính xác")
    void shouldRouteVendorPerformanceCsvExport() {
        UUID vendorId = UUID.randomUUID();
        List<VendorPerformanceItem> items = Collections.emptyList();
        byte[] expectedBytes =
                new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'V', 'E', 'N', 'D'};

        when(getVendorPerformanceReportUseCase.execute(eq(timeRange), eq(vendorId)))
                .thenReturn(items);
        when(csvExporterPort.exportVendorPerformanceReport(eq(items), eq(timeRange)))
                .thenReturn(expectedBytes);

        byte[] result = useCase.execute(ReportType.VENDORS, timeRange, vendorId);

        assertThat(result).isEqualTo(expectedBytes);
        verify(getVendorPerformanceReportUseCase).execute(timeRange, vendorId);
        verify(csvExporterPort).exportVendorPerformanceReport(items, timeRange);
    }

    @Test
    @DisplayName("Ném ngoại lệ nếu type hoặc timeRange null")
    void shouldValidateInputs() {
        assertThatThrownBy(() -> useCase.execute(null, timeRange, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type cannot be null");

        assertThatThrownBy(() -> useCase.execute(ReportType.REVENUE, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("timeRange cannot be null");
    }
}
