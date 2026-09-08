package com.danasea.backend.modules.order.domain.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DiscountCode extends BaseDomainModel {
    private String code;
    private DiscountScope scope;
    private UUID vendorId;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer maxUses;
    private Integer usedCount;
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;
    private Boolean isActive;
}
