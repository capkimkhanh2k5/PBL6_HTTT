package com.danasea.backend.modules.report.presentation.controllers;

import com.danasea.backend.modules.report.application.usecases.GetVendorDashboardUseCase;
import com.danasea.backend.modules.report.domain.models.DashboardMetrics;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.presentation.dtos.VendorDashboardResponse;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.security.infrastructure.SecurityUtils;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorDashboardController {

    private final GetVendorDashboardUseCase getVendorDashboardUseCase;
    private final VendorInternalApi vendorInternalApi;

    @GetMapping
    public ResponseEntity<VendorDashboardResponse> getVendorDashboard(
            @RequestParam(name = "from", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(name = "to", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {

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

        LocalDate today = LocalDate.now(TimeRange.VIETNAM_ZONE);
        LocalDate effectiveFrom = from != null ? from : today.minusDays(30);
        LocalDate effectiveTo = to != null ? to : today;
        TimeRange timeRange = TimeRange.of(effectiveFrom, effectiveTo);

        DashboardMetrics metrics = getVendorDashboardUseCase.execute(vendor.getId(), timeRange);
        return ResponseEntity.ok(VendorDashboardResponse.from(metrics));
    }
}
