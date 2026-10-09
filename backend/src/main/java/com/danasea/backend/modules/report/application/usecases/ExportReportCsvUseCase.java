package com.danasea.backend.modules.report.application.usecases;

import com.danasea.backend.modules.report.application.ports.CsvExporterPort;
import com.danasea.backend.modules.report.domain.enums.GroupByPeriod;
import com.danasea.backend.modules.report.domain.enums.ReportType;
import com.danasea.backend.modules.report.domain.models.BookingReportItem;
import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorPerformanceItem;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * UseCase xuất báo cáo định dạng CSV chuẩn RFC 4180 có UTF-8 BOM cho 3 loại báo cáo (REVENUE,
 * BOOKINGS, VENDORS).
 */
@Service
public class ExportReportCsvUseCase {

    private final GetRevenueReportUseCase getRevenueReportUseCase;
    private final GetBookingReportUseCase getBookingReportUseCase;
    private final GetVendorPerformanceReportUseCase getVendorPerformanceReportUseCase;
    private final CsvExporterPort csvExporterPort;

    public ExportReportCsvUseCase(
            GetRevenueReportUseCase getRevenueReportUseCase,
            GetBookingReportUseCase getBookingReportUseCase,
            GetVendorPerformanceReportUseCase getVendorPerformanceReportUseCase,
            CsvExporterPort csvExporterPort) {
        this.getRevenueReportUseCase =
                Objects.requireNonNull(
                        getRevenueReportUseCase, "getRevenueReportUseCase cannot be null");
        this.getBookingReportUseCase =
                Objects.requireNonNull(
                        getBookingReportUseCase, "getBookingReportUseCase cannot be null");
        this.getVendorPerformanceReportUseCase =
                Objects.requireNonNull(
                        getVendorPerformanceReportUseCase,
                        "getVendorPerformanceReportUseCase cannot be null");
        this.csvExporterPort =
                Objects.requireNonNull(csvExporterPort, "csvExporterPort cannot be null");
    }

    /**
     * Xuất báo cáo sang file CSV theo loại báo cáo yêu cầu.
     *
     * @param type loại báo cáo (REVENUE, BOOKINGS, VENDORS)
     * @param timeRange khoảng thời gian lọc (bắt buộc)
     * @param groupBy chu kỳ gom nhóm thời gian (chỉ áp dụng cho REVENUE và BOOKINGS)
     * @param vendorId ID của vendor (nếu null: toàn hệ thống cho Admin; nếu có: lọc theo vendor)
     * @return mảng byte file CSV có tiền tố UTF-8 BOM (0xEF, 0xBB, 0xBF)
     */
    public byte[] execute(
            ReportType type, TimeRange timeRange, GroupByPeriod groupBy, UUID vendorId) {
        Objects.requireNonNull(type, "report type cannot be null");
        Objects.requireNonNull(timeRange, "timeRange cannot be null");
        GroupByPeriod effectiveGroupBy = groupBy != null ? groupBy : GroupByPeriod.DAY;

        return switch (type) {
            case REVENUE -> {
                List<RevenueReportItem> items =
                        getRevenueReportUseCase.execute(timeRange, effectiveGroupBy, vendorId);
                yield csvExporterPort.exportRevenueReport(items, timeRange);
            }
            case BOOKINGS -> {
                List<BookingReportItem> items =
                        getBookingReportUseCase.execute(timeRange, effectiveGroupBy, vendorId);
                yield csvExporterPort.exportBookingReport(items, timeRange);
            }
            case VENDORS -> {
                List<VendorPerformanceItem> items =
                        getVendorPerformanceReportUseCase.execute(timeRange, vendorId);
                yield csvExporterPort.exportVendorPerformanceReport(items, timeRange);
            }
        };
    }

    public byte[] execute(ReportType type, TimeRange timeRange, UUID vendorId) {
        return execute(type, timeRange, GroupByPeriod.DAY, vendorId);
    }
}
