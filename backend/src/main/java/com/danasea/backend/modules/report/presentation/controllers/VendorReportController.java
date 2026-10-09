package com.danasea.backend.modules.report.presentation.controllers;

import com.danasea.backend.modules.report.application.usecases.ExportReportCsvUseCase;
import com.danasea.backend.modules.report.application.usecases.GetBookingReportUseCase;
import com.danasea.backend.modules.report.application.usecases.GetRevenueReportUseCase;
import com.danasea.backend.modules.report.domain.enums.GroupByPeriod;
import com.danasea.backend.modules.report.domain.enums.ReportType;
import com.danasea.backend.modules.report.domain.models.BookingReportItem;
import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.presentation.dtos.BookingReportResponse;
import com.danasea.backend.modules.report.presentation.dtos.RevenueReportResponse;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.security.infrastructure.SecurityUtils;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorReportController {

    private final GetRevenueReportUseCase getRevenueReportUseCase;
    private final GetBookingReportUseCase getBookingReportUseCase;
    private final ExportReportCsvUseCase exportReportCsvUseCase;
    private final VendorInternalApi vendorInternalApi;

    @GetMapping("/revenue")
    public ResponseEntity<RevenueReportResponse> getRevenueReport(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "groupBy", defaultValue = "day") String groupBy) {

        UUID vendorId = resolveCurrentVendorId();
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
            @RequestParam(name = "groupBy", defaultValue = "day") String groupBy) {

        UUID vendorId = resolveCurrentVendorId();
        TimeRange timeRange = TimeRange.of(from, to);
        GroupByPeriod period = GroupByPeriod.fromString(groupBy);
        List<BookingReportItem> items =
                getBookingReportUseCase.execute(timeRange, period, vendorId);

        return ResponseEntity.ok(BookingReportResponse.of(items, timeRange));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam("type") String type,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "groupBy", defaultValue = "day") String groupBy) {

        UUID vendorId = resolveCurrentVendorId();
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

    private UUID resolveCurrentVendorId() {
        UUID userId =
                SecurityUtils.getCurrentUserId()
                        .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));

        Vendor vendor =
                vendorInternalApi
                        .findByUserId(userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Vendor profile not found for current user"));

        return vendor.getId();
    }
}
