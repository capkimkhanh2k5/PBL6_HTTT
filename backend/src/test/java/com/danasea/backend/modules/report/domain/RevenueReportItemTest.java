package com.danasea.backend.modules.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.danasea.backend.modules.report.domain.models.RevenueReportItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

@DisplayName("RevenueReportItem Financial Integrity & Math Tests")
class RevenueReportItemTest {

    @Test
    @DisplayName(
            "Kiểm chứng công thức tài chính: Collected Cash = GMV - Discount; Net Vendor Payout ="
                    + " Collected Cash - Commission - Refunds")
    void shouldReconcileStandardFinancialFormulas() {
        // Đơn hàng tiêu chuẩn:
        // GMV: 1,000,000 VND
        // Giảm giá (Voucher): 100,000 VND
        // Tiền thực thu từ khách (Collected Cash): 900,000 VND
        // Hoa hồng sàn (10% trên Collected Cash): 90,000 VND
        // Không hoàn tiền: Refunds = 0 VND
        // Tiền thực nhận của vendor: 900,000 - 90,000 - 0 = 810,000 VND
        BigDecimal gmv = BigDecimal.valueOf(1_000_000);
        BigDecimal discount = BigDecimal.valueOf(100_000);
        BigDecimal commission = BigDecimal.valueOf(90_000);
        BigDecimal refunds = BigDecimal.ZERO;
        BigDecimal netPayout = BigDecimal.valueOf(810_000);

        RevenueReportItem item =
                RevenueReportItem.of(
                        "2026-10-01", gmv, discount, refunds, commission, netPayout, 5);

        // Kiểm chứng Collected Cash = GMV - Discount
        assertThat(item.getCollectedCash()).isEqualByComparingTo(gmv.subtract(discount));
        assertThat(item.getCollectedCash()).isEqualByComparingTo("900000");

        // Kiểm chứng Net Vendor Payout = Collected Cash - Commission - Refunds
        BigDecimal expectedPayout = item.getCollectedCash().subtract(commission).subtract(refunds);
        assertThat(item.getNetVendorPayout()).isEqualByComparingTo(expectedPayout);
        assertThat(item.getNetVendorPayout()).isEqualByComparingTo("810000");
    }

    @Test
    @DisplayName(
            "Kiểm chứng toàn vẹn tài chính khi hoàn tiền 100% do WEATHER (sàn không thu hoa hồng,"
                    + " vendor payout = 0)")
    void shouldReconcileWeatherFullRefundScenario() {
        // Kịch bản bão/thời tiết xấu: Hoàn 100% cho khách
        // GMV: 2,500,000 VND
        // Discount: 0 VND
        // Tiền thực thu (Collected Cash): 2,500,000 VND
        // Refunds: 2,500,000 VND (hoàn 100%)
        // Platform Commission: 0 VND (sàn miễn phí hoa hồng khi thời tiết xấu)
        // Net Vendor Payout: 0 VND (chuyến đi không diễn ra, vendor nhận 0đ)
        BigDecimal gmv = BigDecimal.valueOf(2_500_000);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.valueOf(2_500_000);
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal netPayout = BigDecimal.ZERO;

        RevenueReportItem item =
                RevenueReportItem.of(
                        "2026-10-02", gmv, discount, refunds, commission, netPayout, 3);

        assertThat(item.getCollectedCash()).isEqualByComparingTo("2500000");
        assertThat(item.getRefunds()).isEqualByComparingTo("2500000");
        assertThat(item.getPlatformCommission()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getNetVendorPayout()).isEqualByComparingTo(BigDecimal.ZERO);

        // Đối soát: Collected Cash - Refunds - Commission = 2,500,000 - 2,500,000 - 0 = 0 VND
        BigDecimal balance =
                item.getCollectedCash()
                        .subtract(item.getRefunds())
                        .subtract(item.getPlatformCommission());
        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName(
            "Kiểm chứng kịch bản khách hủy trước 24h-48h (hoàn 70%, vendor và sàn chia 30% còn lại"
                    + " theo commission rate)")
    void shouldReconcilePartialCustomerCancellationScenario() {
        // GMV: 1,000,000 VND, không giảm giá -> Collected Cash = 1,000,000 VND
        // Hoàn cho khách 70%: Refunds = 700,000 VND
        // Khoản tiền giữ lại: 300,000 VND
        // Hoa hồng sàn (10% của 300,000): 30,000 VND
        // Thực nhận vendor (90% của 300,000): 270,000 VND
        BigDecimal gmv = BigDecimal.valueOf(1_000_000);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.valueOf(700_000);
        BigDecimal commission = BigDecimal.valueOf(30_000);
        BigDecimal netPayout = BigDecimal.valueOf(270_000);

        RevenueReportItem item =
                RevenueReportItem.of(
                        "2026-10-03", gmv, discount, refunds, commission, netPayout, 1);

        // Đối soát phương trình: Collected Cash = Refunds + Commission + NetPayout
        // 1,000,000 = 700,000 + 30,000 + 270,000
        BigDecimal totalAccounted =
                item.getRefunds().add(item.getPlatformCommission()).add(item.getNetVendorPayout());

        assertThat(item.getCollectedCash()).isEqualByComparingTo(totalAccounted);
        assertThat(item.getNetVendorPayout()).isEqualByComparingTo("270000");
    }

    @Test
    @DisplayName("Tạo data point zero-fill rỗng cho chu kỳ không có giao dịch")
    void shouldCreateZeroFilledItem() {
        RevenueReportItem emptyItem = RevenueReportItem.empty("2026-10-04");

        assertThat(emptyItem.getPeriodKey()).isEqualTo("2026-10-04");
        assertThat(emptyItem.getGmv()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(emptyItem.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(emptyItem.getCollectedCash()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(emptyItem.getRefunds()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(emptyItem.getPlatformCommission()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(emptyItem.getNetVendorPayout()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(emptyItem.getOrderCount()).isZero();
    }
}
