package com.danasea.backend.modules.settlement.domain.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SettlementLineItem extends BaseDomainModel {

    private UUID settlementId;
    private UUID subOrderId;

    /** Giá gốc hoặc doanh thu đối soát của đơn hàng con (sau khấu trừ hoàn tiền) */
    @Builder.Default
    private BigDecimal grossAmount = BigDecimal.ZERO;

    /** Số tiền đã hoàn lại cho khách hàng (0 nếu không hoàn) */
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /** Tỷ lệ hoa hồng hệ thống (ví dụ: 0.1000 = 10%) */
    @Builder.Default
    private BigDecimal commissionRate = BigDecimal.ZERO;

    /** Số tiền hoa hồng = netAfterRefund * commissionRate */
    @Builder.Default
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    /** Số tiền thực chi trả cho Vendor = netAfterRefund - commissionAmount */
    @Builder.Default
    private BigDecimal netAmount = BigDecimal.ZERO;

    /** Lý do loại trừ nếu có */
    @Builder.Default
    private LineItemExclusionReason excludedReason = null;

    public boolean isExcluded() {
        return excludedReason != null && excludedReason != LineItemExclusionReason.NONE && excludedReason.isExcluded();
    }

    /** Tính số tiền thực nhận sau khi trừ tiền hoàn */
    public BigDecimal getNetAfterRefund() {
        BigDecimal gross = grossAmount != null ? grossAmount : BigDecimal.ZERO;
        BigDecimal refund = refundAmount != null ? refundAmount : BigDecimal.ZERO;
        BigDecimal net = gross.subtract(refund);
        return net.compareTo(BigDecimal.ZERO) > 0 ? net : BigDecimal.ZERO;
    }

    /** Factory method tạo line item hợp lệ được thanh toán */
    public static SettlementLineItem createEligible(
            UUID subOrderId,
            BigDecimal grossAmount,
            BigDecimal refundAmount,
            BigDecimal commissionRate) {

        BigDecimal gross = grossAmount != null ? grossAmount : BigDecimal.ZERO;
        BigDecimal refund = refundAmount != null ? refundAmount : BigDecimal.ZERO;
        BigDecimal rate = commissionRate != null ? commissionRate : BigDecimal.ZERO;

        BigDecimal netAfterRefund = gross.subtract(refund);
        if (netAfterRefund.compareTo(BigDecimal.ZERO) < 0) {
            netAfterRefund = BigDecimal.ZERO;
        }

        BigDecimal commission = netAfterRefund.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal payout = netAfterRefund.subtract(commission).setScale(2, RoundingMode.HALF_UP);

        return SettlementLineItem.builder()
                .subOrderId(subOrderId)
                .grossAmount(netAfterRefund)
                .refundAmount(refund)
                .commissionRate(rate)
                .commissionAmount(commission)
                .netAmount(payout)
                .excludedReason(null)
                .build();
    }

    /** Factory method tạo line item bị loại trừ */
    public static SettlementLineItem createExcluded(
            UUID subOrderId,
            BigDecimal grossAmount,
            BigDecimal refundAmount,
            BigDecimal commissionRate,
            LineItemExclusionReason reason) {

        return SettlementLineItem.builder()
                .subOrderId(subOrderId)
                .grossAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .refundAmount(refundAmount != null ? refundAmount : BigDecimal.ZERO)
                .commissionRate(commissionRate != null ? commissionRate : BigDecimal.ZERO)
                .commissionAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .netAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .excludedReason(reason)
                .build();
    }

    // Compatibility aliases
    public BigDecimal getOriginalAmount() {
        return grossAmount;
    }

    public BigDecimal getVendorPayout() {
        return netAmount;
    }
}
