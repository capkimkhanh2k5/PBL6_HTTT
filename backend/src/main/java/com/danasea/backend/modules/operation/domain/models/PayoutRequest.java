package com.danasea.backend.modules.operation.domain.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PayoutRequest extends BaseDomainModel {
    private UUID vendorId;
    private UUID settlementId;
    private BigDecimal amount;
    private PayoutRequestStatus status;
    private UUID processedBy;
    private OffsetDateTime processedAt;
}
