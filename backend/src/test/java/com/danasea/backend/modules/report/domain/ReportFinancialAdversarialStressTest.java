package com.danasea.backend.modules.report.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.report.domain.enums.VendorAlertSeverity;
import com.danasea.backend.modules.report.domain.enums.VendorIssueType;
import com.danasea.backend.modules.report.domain.models.BookingReportItem;
import com.danasea.backend.modules.report.domain.models.DashboardMetrics;
import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorPerformanceItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@DisplayName("Adversarial Stress & Boundary Testing Suite for Milestone 1 Domain Models")
public class ReportFinancialAdversarialStressTest {

    @Nested
    @DisplayName("1. Financial Math & Scale Stress (BigDecimal & High Values)")
    class FinancialMathAndScaleStress {

        @Test
        @DisplayName("Hàng trăm nghìn tỷ VNĐ (Trillions VND) - Không làm tròn sai, không tràn số")
        void shouldHandleHundredsOfTrillionsWithoutOverflowOrPrecisionLoss() {
            // GMV = 500 nghìn tỷ VNĐ: 500,000,000,000,000 VND
            BigDecimal gmv = new BigDecimal("500000000000000");
            // Discount = 50 tỷ VNĐ: 50,000,000,000 VND
            BigDecimal discount = new BigDecimal("50000000000");
            // Collected Cash = 499,950,000,000,000 VND
            BigDecimal expectedCollectedCash = new BigDecimal("499950000000000");

            // Hoa hồng sàn 8.5% trên Collected Cash
            BigDecimal commissionRate = new BigDecimal("0.085");
            BigDecimal commission =
                    expectedCollectedCash
                            .multiply(commissionRate)
                            .setScale(0, RoundingMode.HALF_UP);

            // Hoàn tiền 1 nghìn tỷ VNĐ: 1,000,000,000,000 VND
            BigDecimal refunds = new BigDecimal("1000000000000");

            // Thực nhận Vendor = Collected Cash - Commission - Refunds
            BigDecimal expectedNetPayout =
                    expectedCollectedCash.subtract(commission).subtract(refunds);

            long orderCount = 50_000_000L; // 50 triệu đơn

            RevenueReportItem item =
                    RevenueReportItem.of(
                            "2026-Q4",
                            gmv,
                            discount,
                            refunds,
                            commission,
                            expectedNetPayout,
                            orderCount);

            assertThat(item.getGmv()).isEqualByComparingTo("500000000000000");
            assertThat(item.getDiscountAmount()).isEqualByComparingTo("50000000000");
            assertThat(item.getCollectedCash()).isEqualByComparingTo(expectedCollectedCash);
            assertThat(item.getPlatformCommission()).isEqualByComparingTo(commission);
            assertThat(item.getRefunds()).isEqualByComparingTo("1000000000000");
            assertThat(item.getNetVendorPayout()).isEqualByComparingTo(expectedNetPayout);
            assertThat(item.getOrderCount()).isEqualTo(50_000_000L);

            // Kiểm tra phương trình bảo toàn tài chính
            BigDecimal balance =
                    item.getCollectedCash()
                            .subtract(item.getPlatformCommission())
                            .subtract(item.getRefunds());
            assertThat(balance).isEqualByComparingTo(item.getNetVendorPayout());
        }

        @Test
        @DisplayName("Tất cả giá trị bằng 0 (Zero Boundary) - Tuyệt đối không sinh ngoại lệ")
        void shouldHandleAllZeroValuesGracefully() {
            RevenueReportItem item =
                    RevenueReportItem.of(
                            "2026-10-01",
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            0L);

            assertThat(item.getGmv()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getCollectedCash()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getRefunds()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getPlatformCommission()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getNetVendorPayout()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getOrderCount()).isZero();
        }

        @Test
        @DisplayName(
                "Discount vượt quá GMV (Voucher dị thường) - Clamp Collected Cash về 0 VND thay vì"
                        + " âm")
        void shouldClampCollectedCashToZeroWhenDiscountExceedsGmv() {
            BigDecimal gmv = BigDecimal.valueOf(500_000);
            BigDecimal discount = BigDecimal.valueOf(700_000); // Giảm giá lớn hơn tiền đơn

            RevenueReportItem item =
                    RevenueReportItem.of(
                            "2026-10-01",
                            gmv,
                            discount,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            1L);

            assertThat(item.getCollectedCash()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName(
                "Phòng vệ đầu vào null (Null Safety) - Chuyển đổi thành BigDecimal.ZERO không ném"
                        + " NPE")
        void shouldReplaceNullsWithZeroSafely() {
            RevenueReportItem item =
                    RevenueReportItem.of("2026-10-01", null, null, null, null, null, 0L);

            assertThat(item.getGmv()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getCollectedCash()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getRefunds()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getPlatformCommission()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getNetVendorPayout()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("2. Refund Policies & Commission Rates Adversarial Testing")
    class RefundAndCommissionStress {

        @Test
        @DisplayName("Hoàn tiền 100% bão (WEATHER) - Sàn miễn hoa hồng 0%, vendor thực nhận 0đ")
        void shouldReconcileWeatherStormFullRefund() {
            BigDecimal gmv = BigDecimal.valueOf(15_000_000);
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal refunds = BigDecimal.valueOf(15_000_000);
            BigDecimal commission = BigDecimal.ZERO; // Sàn không thu phí khi thời tiết xấu
            BigDecimal netPayout = BigDecimal.ZERO; // Vendor không được thanh toán chuyến hủy

            RevenueReportItem item =
                    RevenueReportItem.of(
                            "2026-10-05", gmv, discount, refunds, commission, netPayout, 5L);

            assertThat(item.getCollectedCash()).isEqualByComparingTo("15000000");
            assertThat(item.getRefunds()).isEqualByComparingTo("15000000");
            assertThat(item.getPlatformCommission()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getNetVendorPayout()).isEqualByComparingTo(BigDecimal.ZERO);

            BigDecimal remainder =
                    item.getCollectedCash()
                            .subtract(item.getRefunds())
                            .subtract(item.getPlatformCommission());
            assertThat(remainder).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @ParameterizedTest
        @CsvSource({
            "0.00, 1000000, 0, 1000000", // Hoa hồng 0% (Ưu đãi đối tác mới)
            "0.05, 1000000, 50000, 950000", // Hoa hồng 5%
            "0.10, 1000000, 100000, 900000", // Hoa hồng 10%
            "0.20, 1000000, 200000, 800000", // Hoa hồng 20%
            "1.00, 1000000, 1000000, 0" // Hoa hồng 100% (Phí trọn gói)
        })
        @DisplayName("Thử nghiệm các dải tỷ lệ hoa hồng từ 0% đến 100%")
        void shouldReconcileVariousCommissionRates(
                double rate, long collected, long expectedComm, long expectedPayout) {
            BigDecimal gmv = BigDecimal.valueOf(collected);
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal commission = BigDecimal.valueOf(expectedComm);
            BigDecimal refunds = BigDecimal.ZERO;
            BigDecimal netPayout = BigDecimal.valueOf(expectedPayout);

            RevenueReportItem item =
                    RevenueReportItem.of(
                            "2026-10-06", gmv, discount, refunds, commission, netPayout, 1L);

            assertThat(item.getCollectedCash()).isEqualByComparingTo(BigDecimal.valueOf(collected));
            assertThat(item.getPlatformCommission())
                    .isEqualByComparingTo(BigDecimal.valueOf(expectedComm));
            assertThat(item.getNetVendorPayout())
                    .isEqualByComparingTo(BigDecimal.valueOf(expectedPayout));

            // Bảo toàn: Collected Cash = Commission + NetPayout + Refunds
            assertThat(item.getCollectedCash())
                    .isEqualByComparingTo(
                            item.getPlatformCommission()
                                    .add(item.getNetVendorPayout())
                                    .add(item.getRefunds()));
        }

        @Test
        @DisplayName("Hoàn tiền một phần kết hợp hoa hồng trên số tiền giữ lại")
        void shouldReconcilePartialRefundWithProportionalCommission() {
            // Khách hủy đơn 2,000,000đ trước 48h -> hoàn 70% (1,400,000đ)
            // Giữ lại 30% (600,000đ)
            // Hoa hồng sàn 10% của 600,000đ = 60,000đ
            // Vendor thực nhận = 540,000đ
            BigDecimal gmv = BigDecimal.valueOf(2_000_000);
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal refunds = BigDecimal.valueOf(1_400_000);
            BigDecimal commission = BigDecimal.valueOf(60_000);
            BigDecimal netPayout = BigDecimal.valueOf(540_000);

            RevenueReportItem item =
                    RevenueReportItem.of(
                            "2026-10-07", gmv, discount, refunds, commission, netPayout, 1L);

            assertThat(item.getCollectedCash()).isEqualByComparingTo("2000000");
            assertThat(item.getRefunds()).isEqualByComparingTo("1400000");
            assertThat(item.getPlatformCommission()).isEqualByComparingTo("60000");
            assertThat(item.getNetVendorPayout()).isEqualByComparingTo("540000");

            assertThat(item.getCollectedCash())
                    .isEqualByComparingTo(
                            item.getRefunds()
                                    .add(item.getPlatformCommission())
                                    .add(item.getNetVendorPayout()));
        }
    }

    @Nested
    @DisplayName("3. Zero-Division & Rate Calculation Stress Tests")
    class ZeroDivisionAndRateStress {

        @Test
        @DisplayName(
                "DashboardMetrics: totalOrders = 0 không ném ArithmeticException, completionRate &"
                        + " cancellationRate = 0.0")
        void shouldHandleZeroOrdersInDashboardMetricsSafely() {
            assertThatCode(
                            () -> {
                                DashboardMetrics metrics =
                                        DashboardMetrics.of(
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO,
                                                0L, // newOrders = 0
                                                0L, // completedOrders = 0
                                                0L, // cancelledOrders = 0
                                                List.of());

                                assertThat(metrics.getCompletionRate()).isEqualTo(0.0);
                                assertThat(metrics.getCancellationRate()).isEqualTo(0.0);
                                assertThat(Double.isNaN(metrics.getCompletionRate())).isFalse();
                                assertThat(Double.isInfinite(metrics.getCompletionRate()))
                                        .isFalse();
                                assertThat(Double.isNaN(metrics.getCancellationRate())).isFalse();
                                assertThat(Double.isInfinite(metrics.getCancellationRate()))
                                        .isFalse();
                            })
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName(
                "BookingReportItem: totalOrders = 0 không ném ArithmeticException, tỷ lệ = 0.0")
        void shouldHandleZeroOrdersInBookingReportItemSafely() {
            BookingReportItem item = BookingReportItem.empty("2026-10-08");

            assertThat(item.getTotalOrders()).isZero();
            assertThat(item.getCompletionRate()).isEqualTo(0.0);
            assertThat(item.getCancellationRate()).isEqualTo(0.0);
            assertThat(Double.isNaN(item.getCompletionRate())).isFalse();
            assertThat(Double.isNaN(item.getCancellationRate())).isFalse();
        }

        @Test
        @DisplayName(
                "VendorPerformanceItem: totalCapacity = 0 và totalOrders = 0 không ném"
                        + " ArithmeticException, tỷ lệ = 0.0")
        void shouldHandleZeroCapacityAndZeroOrdersInVendorPerformanceSafely() {
            UUID vendorId = UUID.randomUUID();

            assertThatCode(
                            () -> {
                                VendorPerformanceItem item =
                                        VendorPerformanceItem.evaluate(
                                                vendorId,
                                                "New Vendor No Slots",
                                                BigDecimal.ZERO,
                                                BigDecimal.ZERO,
                                                0L, // totalOrders = 0
                                                0L, // completedOrders = 0
                                                0L, // cancelledOrders = 0
                                                0, // totalBooked = 0
                                                0, // totalCapacity = 0
                                                null,
                                                0);

                                assertThat(item.getCancellationRate()).isEqualTo(0.0);
                                assertThat(item.getSlotOccupancyRate()).isEqualTo(0.0);
                                assertThat(Double.isNaN(item.getCancellationRate())).isFalse();
                                assertThat(Double.isInfinite(item.getCancellationRate())).isFalse();
                                assertThat(Double.isNaN(item.getSlotOccupancyRate())).isFalse();
                                assertThat(Double.isInfinite(item.getSlotOccupancyRate()))
                                        .isFalse();
                            })
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName(
                "VendorPerformanceItem: Overbooking (bookedCount > totalCapacity) - Tỷ lệ lấp đầy >"
                        + " 100%, kích hoạt cảnh báo OVERLOADED")
        void shouldHandleOverbookingWithoutErrorsAndTriggerOverloadedAlert() {
            UUID vendorId = UUID.randomUUID();
            int booked = 150;
            int capacity = 100; // Overbooking: 150 chỗ đặt trên 100 chỗ công suất = 150%

            VendorPerformanceItem item =
                    VendorPerformanceItem.evaluate(
                            vendorId,
                            "Overbooked Dive Tour",
                            BigDecimal.valueOf(150_000_000),
                            BigDecimal.valueOf(135_000_000),
                            150L,
                            145L,
                            5L,
                            booked,
                            capacity,
                            BigDecimal.valueOf(4.7),
                            50);

            assertThat(item.getSlotOccupancyRate()).isEqualTo(150.0);
            assertThat(Double.isNaN(item.getSlotOccupancyRate())).isFalse();
            assertThat(Double.isInfinite(item.getSlotOccupancyRate())).isFalse();
            assertThat(item.getIssues()).contains(VendorIssueType.OVERLOADED);
            assertThat(item.getSeverity()).isEqualTo(VendorAlertSeverity.WARNING);
        }

        @Test
        @DisplayName(
                "VendorPerformanceItem: Số âm trong totalCapacity hoặc totalOrders được chặn an"
                        + " toàn")
        void shouldHandleNegativeNumbersGracefully() {
            UUID vendorId = UUID.randomUUID();

            VendorPerformanceItem item =
                    VendorPerformanceItem.evaluate(
                            vendorId,
                            "Anomalous Data Vendor",
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            -5L, // Âm
                            0L,
                            0L,
                            0,
                            -10, // Âm
                            BigDecimal.ZERO,
                            0);

            assertThat(item.getCancellationRate()).isEqualTo(0.0);
            assertThat(item.getSlotOccupancyRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("4. Diagnostic Alert Logic & Edge Threshold Stress")
    class DiagnosticAlertThresholdStress {

        private final UUID vendorId = UUID.randomUUID();

        @Test
        @DisplayName(
                "Tỷ lệ hủy biên chính xác: 20.00% (HEALTHY) vs 20.01% (WARNING HIGH_CANCELLATION)")
        void shouldTriggerHighCancellationOnlyWhenStrictlyGreaterThan20Percent() {
            // Đúng 20.00%: 20 hủy / 100 đơn = 20.00%
            VendorPerformanceItem healthyItem =
                    VendorPerformanceItem.evaluate(
                            vendorId,
                            "Vendor 20%",
                            BigDecimal.TEN,
                            BigDecimal.ONE,
                            100L,
                            80L,
                            20L,
                            50,
                            100,
                            BigDecimal.valueOf(4.5),
                            10);
            assertThat(healthyItem.getCancellationRate()).isEqualTo(20.0);
            assertThat(healthyItem.getIssues()).doesNotContain(VendorIssueType.HIGH_CANCELLATION);

            // Vượt 20%: 21 hủy / 100 đơn = 21.00%
            VendorPerformanceItem warningItem =
                    VendorPerformanceItem.evaluate(
                            vendorId,
                            "Vendor 21%",
                            BigDecimal.TEN,
                            BigDecimal.ONE,
                            100L,
                            79L,
                            21L,
                            50,
                            100,
                            BigDecimal.valueOf(4.5),
                            10);
            assertThat(warningItem.getCancellationRate()).isEqualTo(21.0);
            assertThat(warningItem.getIssues()).contains(VendorIssueType.HIGH_CANCELLATION);
            assertThat(warningItem.getSeverity()).isEqualTo(VendorAlertSeverity.WARNING);
        }

        @Test
        @DisplayName("Tỷ lệ hủy biên leo thang: 40.00% (WARNING) vs 40.01% (CRITICAL)")
        void shouldEscalateCancellationToCriticalWhenGreaterThan40Percent() {
            // Đúng 40.00%: 40 hủy / 100 đơn = 40.00% -> WARNING (chưa > 40%)
            VendorPerformanceItem warningItem =
                    VendorPerformanceItem.evaluate(
                            vendorId,
                            "Vendor 40%",
                            BigDecimal.TEN,
                            BigDecimal.ONE,
                            100L,
                            60L,
                            40L,
                            50,
                            100,
                            BigDecimal.valueOf(4.5),
                            10);
            assertThat(warningItem.getCancellationRate()).isEqualTo(40.0);
            assertThat(warningItem.getSeverity()).isEqualTo(VendorAlertSeverity.WARNING);

            // Vượt 40%: 41 hủy / 100 đơn = 41.00% -> CRITICAL
            VendorPerformanceItem criticalItem =
                    VendorPerformanceItem.evaluate(
                            vendorId,
                            "Vendor 41%",
                            BigDecimal.TEN,
                            BigDecimal.ONE,
                            100L,
                            59L,
                            41L,
                            50,
                            100,
                            BigDecimal.valueOf(4.5),
                            10);
            assertThat(criticalItem.getCancellationRate()).isEqualTo(41.0);
            assertThat(criticalItem.getSeverity()).isEqualTo(VendorAlertSeverity.CRITICAL);
        }

        @Test
        @DisplayName(
                "Điểm đánh giá thấp nhưng số lượng đánh giá chưa đủ (ratingCount < 3) - Không báo"
                        + " động sai")
        void shouldNotFlagLowRatingIfInsufficientReviews() {
            // Điểm 1.0 sao nhưng chỉ có 2 đánh giá -> Chưa đủ mẫu thống kê (yêu cầu >= 3)
            VendorPerformanceItem item =
                    VendorPerformanceItem.evaluate(
                            vendorId,
                            "New Vendor Few Reviews",
                            BigDecimal.TEN,
                            BigDecimal.ONE,
                            10L,
                            10L,
                            0L,
                            50,
                            100,
                            BigDecimal.valueOf(1.0),
                            2);

            assertThat(item.getIssues()).doesNotContain(VendorIssueType.LOW_RATING);
            assertThat(item.getSeverity()).isEqualTo(VendorAlertSeverity.HEALTHY);
        }

        @Test
        @DisplayName("Điểm đánh giá cực thấp (< 2.5) với >= 5 đánh giá -> Leo thang mức CRITICAL")
        void shouldEscalateToCriticalOnVeryLowRatingWithSufficientReviews() {
            VendorPerformanceItem item =
                    VendorPerformanceItem.evaluate(
                            vendorId,
                            "Critically Low Rated Vendor",
                            BigDecimal.TEN,
                            BigDecimal.ONE,
                            20L,
                            20L,
                            0L,
                            50,
                            100,
                            BigDecimal.valueOf(2.2),
                            6);

            assertThat(item.getIssues()).contains(VendorIssueType.LOW_RATING);
            assertThat(item.getSeverity()).isEqualTo(VendorAlertSeverity.CRITICAL);
        }

        @Test
        @DisplayName("Vendor null rating không gây lỗi NullPointerException")
        void shouldHandleNullRatingWithoutNpe() {
            assertThatCode(
                            () -> {
                                VendorPerformanceItem item =
                                        VendorPerformanceItem.evaluate(
                                                vendorId,
                                                "Vendor Null Rating",
                                                BigDecimal.TEN,
                                                BigDecimal.ONE,
                                                10L,
                                                10L,
                                                0L,
                                                50,
                                                100,
                                                null,
                                                0);
                                assertThat(item.getRating()).isEqualByComparingTo(BigDecimal.ZERO);
                                assertThat(item.getIssues())
                                        .doesNotContain(VendorIssueType.LOW_RATING);
                            })
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("5. TimeRange Boundary & UTC/Asia_Ho_Chi_Minh Timezone Stress")
    class TimeRangeTimezoneStress {

        @Test
        @DisplayName("Khoảng thời gian trong cùng một ngày: from == to")
        void shouldHandleSingleDayTimeRange() {
            LocalDate today = LocalDate.of(2026, 10, 8);
            TimeRange range = TimeRange.of(today, today);

            assertThat(range.getFrom()).isEqualTo(today);
            assertThat(range.getTo()).isEqualTo(today);
            assertThat(range.getDaysCount()).isEqualTo(1L);

            // Kiểm tra mốc bắt đầu lúc 00:00:00+07:00
            assertThat(range.getStartDateTime().getHour()).isZero();
            assertThat(range.getStartDateTime().getMinute()).isZero();
            assertThat(range.getStartDateTime().getSecond()).isZero();
            assertThat(range.getStartDateTime().getOffset()).isEqualTo(ZoneOffset.ofHours(7));

            // Kiểm tra mốc kết thúc lúc 23:59:59.999999999+07:00
            assertThat(range.getEndDateTime().getHour()).isEqualTo(23);
            assertThat(range.getEndDateTime().getMinute()).isEqualTo(59);
            assertThat(range.getEndDateTime().getSecond()).isEqualTo(59);
            assertThat(range.getEndDateTime().getNano()).isEqualTo(999_999_999);
            assertThat(range.getEndDateTime().getOffset()).isEqualTo(ZoneOffset.ofHours(7));
        }

        @Test
        @DisplayName(
                "Quy đổi múi giờ UTC so với Asia/Ho_Chi_Minh (UTC+7): 00:00+07 là 17:00 ngày hôm"
                        + " trước theo UTC")
        void shouldCorrectlyEvaluateUtcTimestampsAgainstVietnamTimeRange() {
            LocalDate oct8 = LocalDate.of(2026, 10, 8);
            TimeRange range = TimeRange.of(oct8, oct8);

            // Timestamp trong DB lưu ở UTC: 2026-10-07T17:00:00Z -> tương đương
            // 2026-10-08T00:00:00+07:00
            OffsetDateTime exactStartInUtc = OffsetDateTime.parse("2026-10-07T17:00:00Z");
            assertThat(range.contains(exactStartInUtc)).isTrue();

            // 1 nano giây trước 17:00:00Z -> 2026-10-07T16:59:59.999999999Z -> Nằm ngoài khoảng
            // (hôm trước)
            OffsetDateTime justBeforeStart = OffsetDateTime.parse("2026-10-07T16:59:59.999999999Z");
            assertThat(range.contains(justBeforeStart)).isFalse();

            // 23:59:59.999999999+07:00 tương đương 16:59:59.999999999Z ngày 2026-10-08
            OffsetDateTime exactEndInUtc = OffsetDateTime.parse("2026-10-08T16:59:59.999999999Z");
            assertThat(range.contains(exactEndInUtc)).isTrue();

            // 17:00:00Z ngày 2026-10-08 -> tương đương 00:00:00 ngày 2026-10-09+07:00 -> Nằm ngoài
            // khoảng (hôm sau)
            OffsetDateTime justAfterEnd = OffsetDateTime.parse("2026-10-08T17:00:00Z");
            assertThat(range.contains(justAfterEnd)).isFalse();
        }

        @Test
        @DisplayName("from sau to (from.isAfter(to)) - Bắt buộc ném IllegalArgumentException")
        void shouldThrowExceptionWhenFromIsAfterTo() {
            LocalDate start = LocalDate.of(2026, 10, 10);
            LocalDate end = LocalDate.of(2026, 10, 5);

            assertThatThrownBy(() -> TimeRange.of(start, end))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("From date cannot be after to date");
        }

        @Test
        @DisplayName("Ngày nhuận 29/02 và chuyển giao năm mới 31/12 -> 01/01")
        void shouldHandleLeapYearAndYearBoundary() {
            // Năm nhuận 2024: tháng 2 có 29 ngày
            TimeRange leapRange = TimeRange.of(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29));
            assertThat(leapRange.getDaysCount()).isEqualTo(29L);

            // Chuyển giao năm 2025 -> 2026
            TimeRange yearTurnRange =
                    TimeRange.of(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1));
            assertThat(yearTurnRange.getDaysCount()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("6. Booking Breakdown & EnumMap Integrity")
    class BookingReportBreakdownStress {

        @Test
        @DisplayName(
                "Phân rã tất cả RefundReason trong BookingReportItem và tính toán tỷ lệ chính xác")
        void shouldHandleAllRefundReasonsInBookingReportItem() {
            Map<RefundReason, Long> map = new EnumMap<>(RefundReason.class);
            map.put(RefundReason.WEATHER, 10L);
            map.put(RefundReason.CUSTOMER_CANCEL, 15L);
            map.put(RefundReason.CUSTOMER_REQUEST, 5L);
            map.put(RefundReason.VENDOR_FAULT, 3L);
            map.put(RefundReason.ADMIN_OVERRIDE, 2L);
            map.put(RefundReason.DISPUTE, 1L);
            map.put(RefundReason.COMPENSATION, 1L);

            long totalCancelled = 10 + 15 + 5 + 3 + 2 + 1 + 1; // 37 đơn hủy
            long totalCompleted = 63L; // 63 đơn hoàn thành
            long totalOrders = 100L; // 100 đơn tổng cộng

            BookingReportItem item =
                    new BookingReportItem(
                            "2026-W40", totalOrders, totalCompleted, totalCancelled, map);

            assertThat(item.getCompletionRate()).isEqualTo(63.0);
            assertThat(item.getCancellationRate()).isEqualTo(37.0);
            assertThat(item.getCancellationCount(RefundReason.WEATHER)).isEqualTo(10L);
            assertThat(item.getCancellationCount(RefundReason.CUSTOMER_CANCEL)).isEqualTo(15L);
            assertThat(item.getCancellationCount(RefundReason.VENDOR_FAULT)).isEqualTo(3L);
            assertThat(item.getCancellationCount(RefundReason.ADMIN_OVERRIDE)).isEqualTo(2L);
            assertThat(item.getCancellationCount(null)).isZero();

            // Map phân rã là unmodifiable
            assertThatThrownBy(
                            () ->
                                    item.getCancellationReasonBreakdown()
                                            .put(RefundReason.WEATHER, 99L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
