package com.danasea.backend.modules.report.domain.models;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Một dòng dữ liệu trong báo cáo doanh thu theo chu kỳ thời gian (Time-series revenue item). Bảo
 * đảm tính toàn vẹn của 5 chỉ số tài chính: 1. GMV (Gross Merchandise Value / Booked Amount) 2.
 * Collected Cash = GMV - Discount 3. Refunds (Tiền hoàn lại khách) 4. Platform Commission (Hoa hồng
 * sàn thu) 5. Net Vendor Payout = Collected Cash - Commission - Vendor Refunds
 */
public final class RevenueReportItem {

    private final String periodKey;
    private final BigDecimal gmv;
    private final BigDecimal discountAmount;
    private final BigDecimal collectedCash;
    private final BigDecimal refunds;
    private final BigDecimal platformCommission;
    private final BigDecimal netVendorPayout;
    private final long orderCount;

    public RevenueReportItem(
            String periodKey,
            BigDecimal gmv,
            BigDecimal discountAmount,
            BigDecimal collectedCash,
            BigDecimal refunds,
            BigDecimal platformCommission,
            BigDecimal netVendorPayout,
            long orderCount) {
        this.periodKey = Objects.requireNonNull(periodKey, "periodKey cannot be null");
        this.gmv = gmv != null ? gmv : BigDecimal.ZERO;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.collectedCash = collectedCash != null ? collectedCash : BigDecimal.ZERO;
        this.refunds = refunds != null ? refunds : BigDecimal.ZERO;
        this.platformCommission = platformCommission != null ? platformCommission : BigDecimal.ZERO;
        this.netVendorPayout = netVendorPayout != null ? netVendorPayout : BigDecimal.ZERO;
        this.orderCount = orderCount;
    }

    /** Tạo một dòng báo cáo doanh thu với các chỉ số được tính toán đối soát tự động. */
    public static RevenueReportItem of(
            String periodKey,
            BigDecimal gmv,
            BigDecimal discountAmount,
            BigDecimal refunds,
            BigDecimal platformCommission,
            BigDecimal netVendorPayout,
            long orderCount) {
        BigDecimal safeGmv = gmv != null ? gmv : BigDecimal.ZERO;
        BigDecimal safeDiscount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal safeRefunds = refunds != null ? refunds : BigDecimal.ZERO;
        BigDecimal safeCommission =
                platformCommission != null ? platformCommission : BigDecimal.ZERO;
        BigDecimal safePayout = netVendorPayout != null ? netVendorPayout : BigDecimal.ZERO;

        BigDecimal calculatedCollectedCash = safeGmv.subtract(safeDiscount);
        if (calculatedCollectedCash.compareTo(BigDecimal.ZERO) < 0) {
            calculatedCollectedCash = BigDecimal.ZERO;
        }

        return new RevenueReportItem(
                periodKey,
                safeGmv,
                safeDiscount,
                calculatedCollectedCash,
                safeRefunds,
                safeCommission,
                safePayout,
                orderCount);
    }

    /** Tạo một data point rỗng (zero-fill) cho các chu kỳ không phát sinh đơn hàng. */
    public static RevenueReportItem empty(String periodKey) {
        return new RevenueReportItem(
                periodKey,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L);
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public BigDecimal getGmv() {
        return gmv;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getCollectedCash() {
        return collectedCash;
    }

    public BigDecimal getRefunds() {
        return refunds;
    }

    public BigDecimal getPlatformCommission() {
        return platformCommission;
    }

    public BigDecimal getNetVendorPayout() {
        return netVendorPayout;
    }

    public long getOrderCount() {
        return orderCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RevenueReportItem that = (RevenueReportItem) o;
        return orderCount == that.orderCount
                && Objects.equals(periodKey, that.periodKey)
                && Objects.equals(gmv, that.gmv)
                && Objects.equals(discountAmount, that.discountAmount)
                && Objects.equals(collectedCash, that.collectedCash)
                && Objects.equals(refunds, that.refunds)
                && Objects.equals(platformCommission, that.platformCommission)
                && Objects.equals(netVendorPayout, that.netVendorPayout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                periodKey,
                gmv,
                discountAmount,
                collectedCash,
                refunds,
                platformCommission,
                netVendorPayout,
                orderCount);
    }

    @Override
    public String toString() {
        return "RevenueReportItem{"
                + "periodKey='"
                + periodKey
                + '\''
                + ", gmv="
                + gmv
                + ", discountAmount="
                + discountAmount
                + ", collectedCash="
                + collectedCash
                + ", refunds="
                + refunds
                + ", platformCommission="
                + platformCommission
                + ", netVendorPayout="
                + netVendorPayout
                + ", orderCount="
                + orderCount
                + '}';
    }
}
