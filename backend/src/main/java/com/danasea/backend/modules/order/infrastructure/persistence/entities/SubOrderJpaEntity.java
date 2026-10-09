package com.danasea.backend.modules.order.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "sub_orders")
public class SubOrderJpaEntity extends BaseJpaEntity {

    @Column(name = "booking_item_id")
    private UUID bookingItemId;

    private UUID masterOrderId;

    private UUID vendorId;

    private UUID serviceId;

    private UUID slotId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "vendor_discount_amount")
    private BigDecimal vendorDiscountAmount = BigDecimal.ZERO;

    @Column(name = "platform_discount_amount")
    private BigDecimal platformDiscountAmount = BigDecimal.ZERO;

    @Column(name = "commission_basis_amount")
    private BigDecimal commissionBasisAmount;

    @Column(name = "final_amount")
    private BigDecimal finalAmount;

    private BigDecimal commissionRate;

    private BigDecimal commissionAmount;

    private BigDecimal vendorPayoutAmount;

    @Enumerated(EnumType.STRING)
    private SubOrderStatus status;

    private Boolean waiverAccepted;

    private OffsetDateTime waiverAcceptedAt;

    private UUID qrSecret;

    private OffsetDateTime checkedInAt;

    @Column(name = "vendor_notified_at")
    private OffsetDateTime vendorNotifiedAt;

    public BigDecimal getFinalAmount() {
        if (finalAmount != null) {
            return finalAmount;
        }
        BigDecimal subtotal = subtotalAmount != null ? subtotalAmount : BigDecimal.ZERO;
        return subtotal.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO).max(BigDecimal.ZERO);
    }

    @PrePersist
    @PreUpdate
    private void initializeFinancialSnapshot() {
        if (finalAmount == null) {
            finalAmount = getFinalAmount();
        }
        if (commissionBasisAmount == null) {
            BigDecimal subtotal = subtotalAmount != null ? subtotalAmount : BigDecimal.ZERO;
            commissionBasisAmount = subtotal.subtract(vendorDiscountAmount != null ? vendorDiscountAmount : BigDecimal.ZERO)
                    .max(BigDecimal.ZERO);
        }
    }

}
