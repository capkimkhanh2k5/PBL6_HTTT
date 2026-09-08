package com.danasea.backend.modules.order.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "discount_redemptions")
public class DiscountRedemptionJpaEntity extends BaseJpaEntity {

    private UUID discountCodeId;

    private UUID masterOrderId;

    private BigDecimal amountDeducted;

}
