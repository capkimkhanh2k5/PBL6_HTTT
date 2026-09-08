package com.danasea.backend.modules.order.domain.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SubOrder extends BaseDomainModel {
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
}
