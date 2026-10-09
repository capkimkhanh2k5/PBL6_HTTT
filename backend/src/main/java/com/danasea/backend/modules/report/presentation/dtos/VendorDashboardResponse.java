package com.danasea.backend.modules.report.presentation.dtos;

import com.danasea.backend.modules.report.domain.models.DashboardMetrics;

import java.math.BigDecimal;
import java.util.List;

public record VendorDashboardResponse(
        BigDecimal totalRevenue,
        BigDecimal netPayout,
        long newOrders,
        long completedOrders,
        double completionRate,
        double cancellationRate,
        List<ReportAlertDto> alerts) {

    public static VendorDashboardResponse from(DashboardMetrics metrics) {
        if (metrics == null) {
            return new VendorDashboardResponse(
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0.0, 0.0, List.of());
        }
        List<ReportAlertDto> alertDtos =
                metrics.getAlerts() != null
                        ? metrics.getAlerts().stream().map(ReportAlertDto::from).toList()
                        : List.of();
        return new VendorDashboardResponse(
                metrics.getTotalRevenue(),
                metrics.getNetRevenue(),
                metrics.getNewOrders(),
                metrics.getCompletedOrders(),
                metrics.getCompletionRate(),
                metrics.getCancellationRate(),
                alertDtos);
    }
}
