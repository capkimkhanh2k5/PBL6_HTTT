package com.danasea.backend.security.authorization.presentation;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.report.application.usecases.GetAdminDashboardUseCase;
import com.danasea.backend.modules.report.domain.models.DashboardMetrics;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.presentation.dtos.AdminDashboardResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final GetAdminDashboardUseCase getAdminDashboardUseCase;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardResponse> dashboard(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "vendorId", required = false) UUID vendorId) {
        LocalDate today = LocalDate.now(TimeRange.VIETNAM_ZONE);
        LocalDate effectiveFrom = from != null ? from : today.minusDays(30);
        LocalDate effectiveTo = to != null ? to : today;
        TimeRange timeRange = TimeRange.of(effectiveFrom, effectiveTo);

        DashboardMetrics metrics = getAdminDashboardUseCase.execute(timeRange, vendorId);
        return ResponseEntity.ok(AdminDashboardResponse.from(metrics));
    }
}
