package com.danasea.backend.modules.report.domain.models;

import com.danasea.backend.modules.order.domain.models.RefundReason;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Một dòng dữ liệu trong báo cáo đơn đặt theo chu kỳ thời gian (Time-series booking item). Phân rã
 * chi tiết số đơn hoàn thành, số đơn hủy và phân loại theo lý do hủy.
 */
public final class BookingReportItem {

    private final String periodKey;
    private final long totalOrders;
    private final long completedOrders;
    private final long cancelledOrders;
    private final Map<RefundReason, Long> cancellationReasonBreakdown;

    public BookingReportItem(
            String periodKey,
            long totalOrders,
            long completedOrders,
            long cancelledOrders,
            Map<RefundReason, Long> cancellationReasonBreakdown) {
        this.periodKey = Objects.requireNonNull(periodKey, "periodKey cannot be null");
        this.totalOrders = totalOrders;
        this.completedOrders = completedOrders;
        this.cancelledOrders = cancelledOrders;
        if (cancellationReasonBreakdown != null) {
            Map<RefundReason, Long> map = new EnumMap<>(RefundReason.class);
            map.putAll(cancellationReasonBreakdown);
            this.cancellationReasonBreakdown = Collections.unmodifiableMap(map);
        } else {
            this.cancellationReasonBreakdown = Collections.emptyMap();
        }
    }

    /** Tạo một data point rỗng (zero-fill). */
    public static BookingReportItem empty(String periodKey) {
        return new BookingReportItem(periodKey, 0L, 0L, 0L, Collections.emptyMap());
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public Map<RefundReason, Long> getCancellationReasonBreakdown() {
        return cancellationReasonBreakdown;
    }

    public long getCancellationCount(RefundReason reason) {
        if (reason == null) {
            return 0L;
        }
        return cancellationReasonBreakdown.getOrDefault(reason, 0L);
    }

    public long getUnknownCancellationCount() {
        return Math.max(
                0,
                cancelledOrders
                        - cancellationReasonBreakdown.values().stream()
                                .mapToLong(Long::longValue)
                                .sum());
    }

    public double getCompletionRate() {
        if (totalOrders <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((double) completedOrders / totalOrders * 100.0)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public double getCancellationRate() {
        if (totalOrders <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((double) cancelledOrders / totalOrders * 100.0)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingReportItem that = (BookingReportItem) o;
        return totalOrders == that.totalOrders
                && completedOrders == that.completedOrders
                && cancelledOrders == that.cancelledOrders
                && Objects.equals(periodKey, that.periodKey)
                && Objects.equals(cancellationReasonBreakdown, that.cancellationReasonBreakdown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                periodKey,
                totalOrders,
                completedOrders,
                cancelledOrders,
                cancellationReasonBreakdown);
    }

    @Override
    public String toString() {
        return "BookingReportItem{"
                + "periodKey='"
                + periodKey
                + '\''
                + ", totalOrders="
                + totalOrders
                + ", completedOrders="
                + completedOrders
                + ", cancelledOrders="
                + cancelledOrders
                + ", cancellationReasonBreakdown="
                + cancellationReasonBreakdown
                + '}';
    }
}
