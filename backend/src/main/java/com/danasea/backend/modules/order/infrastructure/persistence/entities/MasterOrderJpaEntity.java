package com.danasea.backend.modules.order.infrastructure.persistence.entities;

import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "master_orders")
public class MasterOrderJpaEntity extends BaseJpaEntity {

    private UUID customerId;

    @Enumerated(EnumType.STRING)
    private MasterOrderStatus status;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private UUID discountCodeId;

}
