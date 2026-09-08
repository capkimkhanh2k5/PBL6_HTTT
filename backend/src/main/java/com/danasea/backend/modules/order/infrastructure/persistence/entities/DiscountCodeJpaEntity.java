package com.danasea.backend.modules.order.infrastructure.persistence.entities;

import com.danasea.backend.modules.order.domain.models.DiscountScope;
import com.danasea.backend.modules.order.domain.models.DiscountType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "discount_codes")
public class DiscountCodeJpaEntity extends BaseJpaEntity {

    private String code;

    @Enumerated(EnumType.STRING)
    private DiscountScope scope;

    private UUID vendorId;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private BigDecimal discountValue;

    private Integer maxUses;

    private Integer usedCount;

    private OffsetDateTime validFrom;

    private OffsetDateTime validTo;

    private Boolean isActive;

}
