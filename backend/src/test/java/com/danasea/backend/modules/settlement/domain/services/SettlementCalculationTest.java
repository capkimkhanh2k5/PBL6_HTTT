package com.danasea.backend.modules.settlement.domain.services;

import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.settlement.domain.models.LineItemExclusionReason;
import com.danasea.backend.modules.settlement.domain.models.Settlement;
import com.danasea.backend.modules.settlement.domain.models.SettlementLineItem;
import com.danasea.backend.modules.settlement.domain.models.SettlementStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SettlementCalculationTest — Comprehensive Tests for EPIC-07 R4")
class SettlementCalculationTest {

    private SettlementCalculationEngine calculationEngine;
    private UUID vendorId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    @BeforeEach
    void setUp() {
        calculationEngine = new SettlementCalculationEngine();
        vendorId = UUID.randomUUID();
        periodStart = LocalDate.of(2026, 9, 1);
        periodEnd = LocalDate.of(2026, 9, 30);
    }

    @Nested
    @DisplayName("1. Quy tắc loại trừ hoàn tiền 100% (Full Refund Exclusion)")
    class FullRefundExclusionTests {

        @Test
        @DisplayName("Đơn REFUNDED toàn phần bị loại trừ hoàn toàn, hoa hồng = 0, excludedReason = FULL_REFUND")
        void testSubOrderWithStatusRefundedIsExcluded() {
            UUID subOrderId = UUID.randomUUID();
            SettlementCalculationEngine.CalculationItem item = new SettlementCalculationEngine.CalculationItem(
                    subOrderId, vendorId, new BigDecimal("1000000.00"), new BigDecimal("0.1000"),
                    SubOrderStatus.REFUNDED, new BigDecimal("1000000.00"), false
            );

            Settlement settlement = calculationEngine.calculate(null, vendorId, periodStart, periodEnd, List.of(item));

            assertThat(settlement.getLineItems()).hasSize(1);
            SettlementLineItem lineItem = settlement.getLineItems().get(0);
            assertThat(lineItem.isExcluded()).isTrue();
            assertThat(lineItem.getExcludedReason()).isEqualTo(LineItemExclusionReason.FULL_REFUND);
            assertThat(lineItem.getGrossAmount()).isEqualByComparingTo("0.00");
            assertThat(lineItem.getCommissionAmount()).isEqualByComparingTo("0.00");
            assertThat(lineItem.getNetAmount()).isEqualByComparingTo("0.00");

            // Không đóng góp vào tổng settlement
            assertThat(settlement.getTotalGrossRevenue()).isEqualByComparingTo("0.00");
            assertThat(settlement.getTotalCommission()).isEqualByComparingTo("0.00");
            assertThat(settlement.getTotalNetPayout()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Đơn COMPLETED nhưng tiền hoàn bằng đúng tiền gốc -> tự động nhận diện FULL_REFUND")
        void testSubOrderWithRefundAmountEqualSubtotalIsTreatedAsFullRefund() {
            UUID subOrderId = UUID.randomUUID();
            SettlementCalculationEngine.CalculationItem item = new SettlementCalculationEngine.CalculationItem(
                    subOrderId, vendorId, new BigDecimal("500000.00"), new BigDecimal("0.1000"),
                    SubOrderStatus.COMPLETED, new BigDecimal("500000.00"), false
            );

            Settlement settlement = calculationEngine.calculate(null, vendorId, periodStart, periodEnd, List.of(item));

            SettlementLineItem lineItem = settlement.getLineItems().get(0);
            assertThat(lineItem.isExcluded()).isTrue();
            assertThat(lineItem.getExcludedReason()).isEqualTo(LineItemExclusionReason.FULL_REFUND);
            assertThat(lineItem.getCommissionAmount()).isEqualByComparingTo("0.00");
            assertThat(lineItem.getNetAmount()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("2. Quy tắc tính hoa hồng sau hoàn tiền (Partial Refund Net Calculation)")
    class PartialRefundCalculationTests {

        @Test
        @DisplayName("Đơn PARTIALLY_REFUNDED tính hoa hồng trên phần thực nhận sau hoàn tiền, không tính trên giá gốc")
        void testPartialRefundCalculatesCommissionOnNetAfterRefund() {
            UUID subOrderId = UUID.randomUUID();
            // Gốc: 2,000,000, Hoàn: 500,000 -> Thực nhận: 1,500,000. Hoa hồng 10% (0.1000)
            SettlementCalculationEngine.CalculationItem item = new SettlementCalculationEngine.CalculationItem(
                    subOrderId, vendorId, new BigDecimal("2000000.00"), new BigDecimal("0.1000"),
                    SubOrderStatus.PARTIALLY_REFUNDED, new BigDecimal("500000.00"), false
            );

            Settlement settlement = calculationEngine.calculate(null, vendorId, periodStart, periodEnd, List.of(item));

            assertThat(settlement.getLineItems()).hasSize(1);
            SettlementLineItem lineItem = settlement.getLineItems().get(0);
            assertThat(lineItem.isExcluded()).isFalse();
            assertThat(lineItem.getExcludedReason()).isNull();
            assertThat(lineItem.getGrossAmount()).isEqualByComparingTo("1500000.00");
            assertThat(lineItem.getCommissionAmount()).isEqualByComparingTo("150000.00");
            assertThat(lineItem.getNetAmount()).isEqualByComparingTo("1350000.00");
            assertThat(lineItem.getRefundAmount()).isEqualByComparingTo("500000.00");

            // Kiểm tra tổng kỳ khớp chính xác
            assertThat(settlement.getTotalGrossRevenue()).isEqualByComparingTo("1500000.00");
            assertThat(settlement.getTotalCommission()).isEqualByComparingTo("150000.00");
            assertThat(settlement.getTotalNetPayout()).isEqualByComparingTo("1350000.00");
        }

        @Test
        @DisplayName("Hoàn tiền tỷ lệ lớn 99.99%: Đơn 1,000,000 hoàn 999,900 -> Thực nhận 100, hoa hồng 10.00, net 90.00")
        void testPartialRefundSmallNet() {
            UUID subOrderId = UUID.randomUUID();
            SettlementCalculationEngine.CalculationItem item = new SettlementCalculationEngine.CalculationItem(
                    subOrderId, vendorId, new BigDecimal("1000000.00"), new BigDecimal("0.1000"),
                    SubOrderStatus.PARTIALLY_REFUNDED, new BigDecimal("999900.00"), false
            );

            Settlement settlement = calculationEngine.calculate(null, vendorId, periodStart, periodEnd, List.of(item));

            SettlementLineItem lineItem = settlement.getLineItems().get(0);
            assertThat(lineItem.isExcluded()).isFalse();
            assertThat(lineItem.getGrossAmount()).isEqualByComparingTo("100.00");
            assertThat(lineItem.getCommissionAmount()).isEqualByComparingTo("10.00");
            assertThat(lineItem.getNetAmount()).isEqualByComparingTo("90.00");
        }

        @Test
        @DisplayName("Hoàn tiền tỷ lệ nhỏ 0.01%: Đơn 1,000,000 hoàn 100 -> Thực nhận 999,900, hoa hồng 99,990.00, net 899,910.00")
        void testPartialRefundLargeNet() {
            UUID subOrderId = UUID.randomUUID();
            SettlementCalculationEngine.CalculationItem item = new SettlementCalculationEngine.CalculationItem(
                    subOrderId, vendorId, new BigDecimal("1000000.00"), new BigDecimal("0.1000"),
                    SubOrderStatus.PARTIALLY_REFUNDED, new BigDecimal("100.00"), false
            );

            Settlement settlement = calculationEngine.calculate(null, vendorId, periodStart, periodEnd, List.of(item));

            SettlementLineItem lineItem = settlement.getLineItems().get(0);
            assertThat(lineItem.isExcluded()).isFalse();
            assertThat(lineItem.getGrossAmount()).isEqualByComparingTo("999900.00");
            assertThat(lineItem.getCommissionAmount()).isEqualByComparingTo("99990.00");
            assertThat(lineItem.getNetAmount()).isEqualByComparingTo("899910.00");
        }
    }

    @Nested
    @DisplayName("3. Quy tắc tạm giữ khiếu nại đang mở (Active Dispute Hold)")
    class ActiveDisputeHoldTests {

        @Test
        @DisplayName("Đơn có khiếu nại đang mở (hasActiveDispute = true) bị tạm loại khỏi kỳ settlement")
        void testActiveDisputeSubOrderIsExcluded() {
            UUID subOrderId = UUID.randomUUID();
            SettlementCalculationEngine.CalculationItem item = new SettlementCalculationEngine.CalculationItem(
                    subOrderId, vendorId, new BigDecimal("1500000.00"), new BigDecimal("0.1000"),
                    SubOrderStatus.COMPLETED, BigDecimal.ZERO, true
            );

            Settlement settlement = calculationEngine.calculate(null, vendorId, periodStart, periodEnd, List.of(item));

            SettlementLineItem lineItem = settlement.getLineItems().get(0);
            assertThat(lineItem.isExcluded()).isTrue();
            assertThat(lineItem.getExcludedReason()).isEqualTo(LineItemExclusionReason.ACTIVE_DISPUTE);
            assertThat(lineItem.getGrossAmount()).isEqualByComparingTo("0.00");
            assertThat(lineItem.getCommissionAmount()).isEqualByComparingTo("0.00");
            assertThat(lineItem.getNetAmount()).isEqualByComparingTo("0.00");
            assertThat(settlement.getTotalGrossRevenue()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Đơn sau khi khiếu nại đã giải quyết (hasActiveDispute = false) được đưa vào tính hoa hồng bình thường")
        void testResolvedDisputeSubOrderIsCalculated() {
            UUID subOrderId = UUID.randomUUID();
            SettlementCalculationEngine.CalculationItem item = new SettlementCalculationEngine.CalculationItem(
                    subOrderId, vendorId, new BigDecimal("1500000.00"), new BigDecimal("0.1000"),
                    SubOrderStatus.COMPLETED, BigDecimal.ZERO, false
            );

            Settlement settlement = calculationEngine.calculate(null, vendorId, periodStart, periodEnd, List.of(item));

            SettlementLineItem lineItem = settlement.getLineItems().get(0);
            assertThat(lineItem.isExcluded()).isFalse();
            assertThat(lineItem.getExcludedReason()).isNull();
            assertThat(lineItem.getCommissionAmount()).isEqualByComparingTo("150000.00");
            assertThat(lineItem.getNetAmount()).isEqualByComparingTo("1350000.00");
            assertThat(settlement.getTotalGrossRevenue()).isEqualByComparingTo("1500000.00");
        }
    }

    @Nested
    @DisplayName("4. Quy tắc đơn tiêu chuẩn & Trạng thái không hợp lệ (Standard & Non-eligible Orders)")
    class StandardAndNonEligibleOrderTests {

        @Test
        @DisplayName("Đơn CHECKED_IN hoặc COMPLETED không hoàn tiền, không khiếu nại tính hoa hồng đầy đủ")
        void testStandardCheckedInOrderCalculatesNormally() {
            UUID subOrderId = UUID.randomUUID();
            SettlementCalculationEngine.CalculationItem item = new SettlementCalculationEngine.CalculationItem(
                    subOrderId, vendorId, new BigDecimal("800000.00"), new BigDecimal("0.1000"),
                    SubOrderStatus.CHECKED_IN, BigDecimal.ZERO, false
            );

            Settlement settlement = calculationEngine.calculate(null, vendorId, periodStart, periodEnd, List.of(item));

            SettlementLineItem lineItem = settlement.getLineItems().get(0);
            assertThat(lineItem.isExcluded()).isFalse();
            assertThat(lineItem.getGrossAmount()).isEqualByComparingTo("800000.00");
            assertThat(lineItem.getCommissionAmount()).isEqualByComparingTo("80000.00");
            assertThat(lineItem.getNetAmount()).isEqualByComparingTo("720000.00");
        }

        @Test
        @DisplayName("Đơn PENDING hoặc CANCELLED bị đánh dấu NOT_ELIGIBLE và loại khỏi đối soát")
        void testNonEligibleStatusExcluded() {
            UUID subOrderId1 = UUID.randomUUID();
            SettlementCalculationEngine.CalculationItem item1 = new SettlementCalculationEngine.CalculationItem(
                    subOrderId1, vendorId, new BigDecimal("500000.00"), new BigDecimal("0.1000"),
                    SubOrderStatus.PENDING, BigDecimal.ZERO, false
            );

            UUID subOrderId2 = UUID.randomUUID();
            SettlementCalculationEngine.CalculationItem item2 = new SettlementCalculationEngine.CalculationItem(
                    subOrderId2, vendorId, new BigDecimal("500000.00"), new BigDecimal("0.1000"),
                    SubOrderStatus.CANCELLED, BigDecimal.ZERO, false
            );

            Settlement settlement = calculationEngine.calculate(null, vendorId, periodStart, periodEnd, List.of(item1, item2));

            assertThat(settlement.getLineItems()).hasSize(2);
            assertThat(settlement.getLineItems().get(0).getExcludedReason()).isEqualTo(LineItemExclusionReason.NOT_ELIGIBLE);
            assertThat(settlement.getLineItems().get(1).getExcludedReason()).isEqualTo(LineItemExclusionReason.NOT_ELIGIBLE);
            assertThat(settlement.getTotalGrossRevenue()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("5. Định lý kế toán cân bằng & Làm tròn số học (Financial Invariant & Rounding)")
    class FinancialInvariantTests {

        @Test
        @DisplayName("Làm tròn HALF_UP scale 2: 100.05 VND với tỷ lệ 15% (0.1500) -> hoa hồng 15.01, net 85.04 (Tổng = 100.05)")
        void testHalfUpRoundingInvariant() {
            UUID subOrderId = UUID.randomUUID();
            // 100.05 * 0.1500 = 15.0075 -> rounds to 15.01. Net = 100.05 - 15.01 = 85.04
            SettlementCalculationEngine.CalculationItem item = new SettlementCalculationEngine.CalculationItem(
                    subOrderId, vendorId, new BigDecimal("100.05"), new BigDecimal("0.1500"),
                    SubOrderStatus.COMPLETED, BigDecimal.ZERO, false
            );

            Settlement settlement = calculationEngine.calculate(null, vendorId, periodStart, periodEnd, List.of(item));

            SettlementLineItem lineItem = settlement.getLineItems().get(0);
            assertThat(lineItem.getCommissionAmount()).isEqualByComparingTo("15.01");
            assertThat(lineItem.getNetAmount()).isEqualByComparingTo("85.04");
            assertThat(lineItem.getGrossAmount()).isEqualByComparingTo("100.05");

            // Invariant: gross = commission + net
            assertThat(lineItem.getGrossAmount()).isEqualTo(lineItem.getCommissionAmount().add(lineItem.getNetAmount()));
        }

        @Test
        @DisplayName("Đơn hàng tổng hợp nhiều loại giữ vững cân bằng kế toán tổng toàn kỳ")
        void testMixedOrdersSettlementAggregation() {
            UUID s1 = UUID.randomUUID(); // Standard: 1,000,000, commission 10% = 100,000, net = 900,000
            UUID s2 = UUID.randomUUID(); // Partial: 2,000,000 - 500,000 = 1,500,000, commission 10% = 150,000, net = 1,350,000
            UUID s3 = UUID.randomUUID(); // Full refund: excluded (0, 0, 0)
            UUID s4 = UUID.randomUUID(); // Dispute active: excluded (0, 0, 0)

            List<SettlementCalculationEngine.CalculationItem> items = List.of(
                    new SettlementCalculationEngine.CalculationItem(s1, vendorId, new BigDecimal("1000000.00"), new BigDecimal("0.1000"), SubOrderStatus.COMPLETED, BigDecimal.ZERO, false),
                    new SettlementCalculationEngine.CalculationItem(s2, vendorId, new BigDecimal("2000000.00"), new BigDecimal("0.1000"), SubOrderStatus.PARTIALLY_REFUNDED, new BigDecimal("500000.00"), false),
                    new SettlementCalculationEngine.CalculationItem(s3, vendorId, new BigDecimal("800000.00"), new BigDecimal("0.1000"), SubOrderStatus.REFUNDED, new BigDecimal("800000.00"), false),
                    new SettlementCalculationEngine.CalculationItem(s4, vendorId, new BigDecimal("1200000.00"), new BigDecimal("0.1000"), SubOrderStatus.COMPLETED, BigDecimal.ZERO, true)
            );

            Settlement settlement = calculationEngine.calculate(null, vendorId, periodStart, periodEnd, items);

            // Gross = 1,000,000 + 1,500,000 = 2,500,000.00
            // Commission = 100,000 + 150,000 = 250,000.00
            // Net = 900,000 + 1,350,000 = 2,250,000.00
            assertThat(settlement.getTotalGrossRevenue()).isEqualByComparingTo("2500000.00");
            assertThat(settlement.getTotalCommission()).isEqualByComparingTo("250000.00");
            assertThat(settlement.getTotalNetPayout()).isEqualByComparingTo("2250000.00");

            // Accounting Invariant: Gross = Commission + Net
            assertThat(settlement.getTotalGrossRevenue()).isEqualTo(
                    settlement.getTotalCommission().add(settlement.getTotalNetPayout())
            );
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.DRAFT);
        }

        @Test
        @DisplayName("Danh sách đơn trống trả về settlement với các tổng = 0.00")
        void testEmptyItemsListReturnsZeroTotals() {
            Settlement settlement = calculationEngine.calculate(null, vendorId, periodStart, periodEnd, Collections.emptyList());

            assertThat(settlement.getLineItems()).isEmpty();
            assertThat(settlement.getTotalGrossRevenue()).isEqualByComparingTo("0.00");
            assertThat(settlement.getTotalCommission()).isEqualByComparingTo("0.00");
            assertThat(settlement.getTotalNetPayout()).isEqualByComparingTo("0.00");
        }
    }
}
