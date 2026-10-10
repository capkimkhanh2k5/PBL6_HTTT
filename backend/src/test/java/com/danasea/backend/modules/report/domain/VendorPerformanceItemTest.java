package com.danasea.backend.modules.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.danasea.backend.modules.report.domain.enums.VendorAlertSeverity;
import com.danasea.backend.modules.report.domain.enums.VendorIssueType;
import com.danasea.backend.modules.report.domain.models.VendorPerformanceItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

@DisplayName("VendorPerformanceItem Diagnostic & Evaluation Tests")
class VendorPerformanceItemTest {

    private final UUID vendorId = UUID.randomUUID();

    @Test
    @DisplayName("Kiểm chứng xử lý slot occupancy rate tránh chia cho 0 khi totalCapacity = 0")
    void shouldHandleZeroSlotCapacityWithoutDivisionByZero() {
        // Vendor mới chưa mở slot hoặc capacity = 0
        VendorPerformanceItem item =
                VendorPerformanceItem.evaluate(
                        vendorId,
                        "Vendor Zero Capacity",
                        BigDecimal.valueOf(10_000_000),
                        BigDecimal.valueOf(9_000_000),
                        10,
                        10,
                        0,
                        0, // totalBooked
                        0, // totalCapacity = 0
                        BigDecimal.valueOf(4.5),
                        10);

        // Tỷ lệ lấp đầy phải là 0.0%, tuyệt đối không được phát sinh NaN hay ngoại lệ
        assertThat(item.getSlotOccupancyRate()).isEqualTo(0.0);
        assertThat(Double.isNaN(item.getSlotOccupancyRate())).isFalse();
        assertThat(Double.isInfinite(item.getSlotOccupancyRate())).isFalse();
    }

    @Test
    @DisplayName("Tính toán tỷ lệ lấp đầy slot thông thường chính xác")
    void shouldCalculateNormalSlotOccupancyRate() {
        // 80 booked / 100 capacity = 80.0%
        VendorPerformanceItem item =
                VendorPerformanceItem.evaluate(
                        vendorId,
                        "Vendor Normal Capacity",
                        BigDecimal.valueOf(50_000_000),
                        BigDecimal.valueOf(45_000_000),
                        50,
                        48,
                        2,
                        80, // totalBooked
                        100, // totalCapacity
                        BigDecimal.valueOf(4.8),
                        20);

        assertThat(item.getSlotOccupancyRate()).isEqualTo(80.0);
        assertThat(item.getCancellationRate()).isEqualTo(4.0); // 2/50 * 100 = 4%
        assertThat(item.getSeverity()).isEqualTo(VendorAlertSeverity.HEALTHY);
        assertThat(item.getIssues()).isEmpty();
    }

    @Test
    @DisplayName("Phát hiện cảnh báo OVERLOADED khi slot occupancy >= 95%")
    void shouldDetectOverloadedWhenOccupancyRateIsAtLeast95Percent() {
        // 98 booked / 100 capacity = 98.0%
        VendorPerformanceItem item =
                VendorPerformanceItem.evaluate(
                        vendorId,
                        "Vendor Peak Season",
                        BigDecimal.valueOf(100_000_000),
                        BigDecimal.valueOf(90_000_000),
                        100,
                        95,
                        5,
                        98,
                        100,
                        BigDecimal.valueOf(4.6),
                        30);

        assertThat(item.getSlotOccupancyRate()).isEqualTo(98.0);
        assertThat(item.getIssues()).contains(VendorIssueType.OVERLOADED);
        assertThat(item.getSeverity()).isEqualTo(VendorAlertSeverity.WARNING);
    }

    @Test
    @DisplayName("Phát hiện cảnh báo HIGH_CANCELLATION khi tỷ lệ hủy > 20% và CRITICAL khi > 40%")
    void shouldDetectHighCancellationAndEscalateToCritical() {
        // Tỷ lệ hủy 25% (> 20%) -> WARNING
        VendorPerformanceItem warningItem =
                VendorPerformanceItem.evaluate(
                        vendorId,
                        "Vendor Warning Cancellation",
                        BigDecimal.valueOf(20_000_000),
                        BigDecimal.valueOf(18_000_000),
                        40,
                        30,
                        10, // 10/40 = 25%
                        50,
                        100,
                        BigDecimal.valueOf(4.0),
                        15);

        assertThat(warningItem.getCancellationRate()).isEqualTo(25.0);
        assertThat(warningItem.getIssues()).contains(VendorIssueType.HIGH_CANCELLATION);
        assertThat(warningItem.getSeverity()).isEqualTo(VendorAlertSeverity.WARNING);

        // Tỷ lệ hủy 45% (> 40%) -> CRITICAL
        VendorPerformanceItem criticalItem =
                VendorPerformanceItem.evaluate(
                        vendorId,
                        "Vendor Critical Cancellation",
                        BigDecimal.valueOf(10_000_000),
                        BigDecimal.valueOf(9_000_000),
                        20,
                        11,
                        9, // 9/20 = 45%
                        30,
                        100,
                        BigDecimal.valueOf(4.0),
                        10);

        assertThat(criticalItem.getCancellationRate()).isEqualTo(45.0);
        assertThat(criticalItem.getIssues()).contains(VendorIssueType.HIGH_CANCELLATION);
        assertThat(criticalItem.getSeverity()).isEqualTo(VendorAlertSeverity.CRITICAL);
    }

    @Test
    @DisplayName("Phát hiện cảnh báo LOW_RATING khi điểm đánh giá < 3.5")
    void shouldDetectLowRatingIssue() {
        VendorPerformanceItem item =
                VendorPerformanceItem.evaluate(
                        vendorId,
                        "Vendor Poor Service",
                        BigDecimal.valueOf(15_000_000),
                        BigDecimal.valueOf(13_500_000),
                        30,
                        28,
                        2,
                        50,
                        100,
                        BigDecimal.valueOf(3.2), // < 3.5
                        5 // >= 3 reviews
                        );

        assertThat(item.getIssues()).contains(VendorIssueType.LOW_RATING);
        assertThat(item.getSeverity()).isEqualTo(VendorAlertSeverity.WARNING);
    }

    @Test
    @DisplayName("Phát hiện cảnh báo UNDERPERFORMING khi lấp đầy < 20% và đơn < 5")
    void shouldDetectUnderperformingIssue() {
        VendorPerformanceItem item =
                VendorPerformanceItem.evaluate(
                        vendorId,
                        "Vendor Slow Start",
                        BigDecimal.valueOf(2_000_000),
                        BigDecimal.valueOf(1_800_000),
                        3, // < 5 đơn
                        3,
                        0,
                        10, // 10 booked
                        100, // 100 capacity -> 10% (< 20%)
                        BigDecimal.valueOf(4.5),
                        2);

        assertThat(item.getIssues()).contains(VendorIssueType.UNDERPERFORMING);
        assertThat(item.getSeverity()).isEqualTo(VendorAlertSeverity.WARNING);
    }
}
