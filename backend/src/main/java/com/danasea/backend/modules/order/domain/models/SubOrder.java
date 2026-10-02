package com.danasea.backend.modules.order.domain.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SubOrder extends BaseDomainModel {

    private UUID bookingItemId;
    private UUID masterOrderId;
    private UUID vendorId;
    private UUID serviceId;
    private UUID slotId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotalAmount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal vendorPayoutAmount;
    private SubOrderStatus status;
    private Boolean waiverAccepted;
    private OffsetDateTime waiverAcceptedAt;
    private UUID qrSecret;
    private OffsetDateTime checkedInAt;
    private OffsetDateTime vendorNotifiedAt;

    public SubOrder() {
        super();
        this.status = SubOrderStatus.PENDING;
        this.waiverAccepted = false;
    }

    /**
     * Tự bảo vệ tính đúng đắn khi chuyển trạng thái của SubOrder.
     */
    public void assertValidTransition(SubOrderStatus newStatus) {
        if (this.status == newStatus) {
            return;
        }
        if (this.status != null && this.status.isTerminal()) {
            throw new InvalidOrderStateException(
                    "Cannot transition sub-order from terminal state " + this.status + " to " + newStatus);
        }
        if (this.status == SubOrderStatus.PENDING) {
            if (newStatus != SubOrderStatus.CONFIRMED
                    && newStatus != SubOrderStatus.CANCELLED
                    && newStatus != SubOrderStatus.REJECTED) {
                throw new InvalidOrderStateException(
                        "SubOrder in PENDING can only transition to CONFIRMED, CANCELLED, or REJECTED; requested: " + newStatus);
            }
        }
    }

    public void markConfirmed() {
        assertValidTransition(SubOrderStatus.CONFIRMED);
        this.status = SubOrderStatus.CONFIRMED;
    }

    public void cancel() {
        assertValidTransition(SubOrderStatus.CANCELLED);
        this.status = SubOrderStatus.CANCELLED;
    }

    public void markVendorNotified(OffsetDateTime timestamp) {
        this.vendorNotifiedAt = timestamp;
    }
}
