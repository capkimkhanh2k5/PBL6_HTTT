package com.danasea.backend.modules.order.domain.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Refund extends BaseDomainModel {
    private UUID subOrderId;
    private BigDecimal amount;
    private BigDecimal refundPercentage;
    private RefundReason reason;
    private RefundStatus status;
    private OffsetDateTime processedAt;
}
