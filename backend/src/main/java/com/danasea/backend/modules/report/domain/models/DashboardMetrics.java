package com.danasea.backend.modules.report.domain.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Chỉ số hoạt động tổng quan (Real-time Dashboard Metrics) cho Admin hoặc Vendor. */
public final class DashboardMetrics {

    private final BigDecimal totalRevenue;
    private final BigDecimal netRevenue;
    private final long newOrders;
    private final long completedOrders;
    private final long cancelledOrders;
    private final double completionRate;
    private final double cancellationRate;
    private final List<VendorDiagnosisAlert> alerts;

    public DashboardMetrics(
            BigDecimal totalRevenue,
            BigDecimal netRevenue,
            long newOrders,
            long completedOrders,
            long cancelledOrders,
            double completionRate,
            double cancellationRate,
            List<VendorDiagnosisAlert> alerts) {
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        this.netRevenue = netRevenue != null ? netRevenue : BigDecimal.ZERO;
        this.newOrders = newOrders;
        this.completedOrders = completedOrders;
        this.cancelledOrders = cancelledOrders;
        this.completionRate = completionRate;
        this.cancellationRate = cancellationRate;
        this.alerts =
                alerts != null
                        ? Collections.unmodifiableList(new ArrayList<>(alerts))
                        : Collections.emptyList();
    }

    /** Tạo DashboardMetrics với tính toán tỷ lệ tự động và an toàn (tránh chia cho 0). */
    public static DashboardMetrics of(
            BigDecimal totalRevenue,
            BigDecimal netRevenue,
            long newOrders,
            long completedOrders,
            long cancelledOrders,
            List<VendorDiagnosisAlert> alerts) {
        long totalOrders = newOrders;
        if (totalOrders <= 0) {
            totalOrders = completedOrders + cancelledOrders;
        }

        double completionRate = 0.0;
        double cancellationRate = 0.0;
        if (totalOrders > 0) {
            completionRate =
                    BigDecimal.valueOf((double) completedOrders / totalOrders * 100.0)
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue();
            cancellationRate =
                    BigDecimal.valueOf((double) cancelledOrders / totalOrders * 100.0)
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue();
        }

        return new DashboardMetrics(
                totalRevenue,
                netRevenue,
                newOrders,
                completedOrders,
                cancelledOrders,
                completionRate,
                cancellationRate,
                alerts);
    }

    public static DashboardMetrics empty() {
        return new DashboardMetrics(
                BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, 0.0, 0.0, Collections.emptyList());
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public BigDecimal getNetRevenue() {
        return netRevenue;
    }

    public long getNewOrders() {
        return newOrders;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public double getCancellationRate() {
        return cancellationRate;
    }

    public List<VendorDiagnosisAlert> getAlerts() {
        return alerts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardMetrics that = (DashboardMetrics) o;
        return newOrders == that.newOrders
                && completedOrders == that.completedOrders
                && cancelledOrders == that.cancelledOrders
                && Double.compare(that.completionRate, completionRate) == 0
                && Double.compare(that.cancellationRate, cancellationRate) == 0
                && Objects.equals(totalRevenue, that.totalRevenue)
                && Objects.equals(netRevenue, that.netRevenue)
                && Objects.equals(alerts, that.alerts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                totalRevenue,
                netRevenue,
                newOrders,
                completedOrders,
                cancelledOrders,
                completionRate,
                cancellationRate,
                alerts);
    }

    @Override
    public String toString() {
        return "DashboardMetrics{"
                + "totalRevenue="
                + totalRevenue
                + ", netRevenue="
                + netRevenue
                + ", newOrders="
                + newOrders
                + ", completedOrders="
                + completedOrders
                + ", cancelledOrders="
                + cancelledOrders
                + ", completionRate="
                + completionRate
                + ", cancellationRate="
                + cancellationRate
                + ", alertsCount="
                + alerts.size()
                + '}';
    }
}
