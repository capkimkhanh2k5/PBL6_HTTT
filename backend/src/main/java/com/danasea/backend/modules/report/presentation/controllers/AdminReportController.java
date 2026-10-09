package com.danasea.backend.modules.report.presentation.controllers;

import com.danasea.backend.modules.report.application.usecases.ExportReportCsvUseCase;
import com.danasea.backend.modules.report.application.usecases.GetBookingReportUseCase;
import com.danasea.backend.modules.report.application.usecases.GetRevenueReportUseCase;
import com.danasea.backend.modules.report.application.usecases.GetVendorPerformanceReportUseCase;
import com.danasea.backend.modules.report.domain.enums.GroupByPeriod;
import com.danasea.backend.modules.report.domain.enums.ReportType;
import com.danasea.backend.modules.report.domain.models.BookingReportItem;
import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorPerformanceItem;
import com.danasea.backend.modules.report.presentation.dtos.BookingReportResponse;
import com.danasea.backend.modules.report.presentation.dtos.RevenueReportResponse;
import com.danasea.backend.modules.report.presentation.dtos.VendorPerformanceResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final GetRevenueReportUseCase getRevenueReportUseCase;
    private final GetBookingReportUseCase getBookingReportUseCase;
    private final GetVendorPerformanceReportUseCase getVendorPerformanceReportUseCase;
    private final ExportReportCsvUseCase exportReportCsvUseCase;

    @GetMapping("/revenue")
    public ResponseEntity<RevenueReportResponse> getRevenueReport(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "groupBy", defaultValue = "day") String groupBy,
            @RequestParam(name = "vendorId", required = false) UUID vendorId) {

        TimeRange timeRange = TimeRange.of(from, to);
        GroupByPeriod period = GroupByPeriod.fromString(groupBy);
        List<RevenueReportItem> items =
                getRevenueReportUseCase.execute(timeRange, period, vendorId);

        return ResponseEntity.ok(RevenueReportResponse.of(items, timeRange));
    }

    @GetMapping("/bookings")
    public ResponseEntity<BookingReportResponse> getBookingReport(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "groupBy", defaultValue = "day") String groupBy,
            @RequestParam(name = "vendorId", required = false) UUID vendorId) {

        TimeRange timeRange = TimeRange.of(from, to);
        GroupByPeriod period = GroupByPeriod.fromString(groupBy);
        List<BookingReportItem> items =
                getBookingReportUseCase.execute(timeRange, period, vendorId);

        return ResponseEntity.ok(BookingReportResponse.of(items, timeRange));
    }

    @GetMapping("/vendors")
    public ResponseEntity<VendorPerformanceResponse> getVendorPerformanceReport(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "vendorId", required = false) UUID vendorId) {

        TimeRange timeRange = TimeRange.of(from, to);
        List<VendorPerformanceItem> items =
                getVendorPerformanceReportUseCase.execute(timeRange, vendorId);

        return ResponseEntity.ok(VendorPerformanceResponse.of(items, timeRange));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam("type") String type,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "groupBy", defaultValue = "day") String groupBy,
            @RequestParam(name = "vendorId", required = false) UUID vendorId) {

        TimeRange timeRange = TimeRange.of(from, to);
        ReportType reportType = ReportType.fromString(type);
        GroupByPeriod period = GroupByPeriod.fromString(groupBy);
        byte[] csvBytes = exportReportCsvUseCase.execute(reportType, timeRange, period, vendorId);

        String filename =
                "report-" + reportType.name().toLowerCase() + "-" + from + "-" + to + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        return ResponseEntity.ok().headers(headers).body(csvBytes);
    }
}
