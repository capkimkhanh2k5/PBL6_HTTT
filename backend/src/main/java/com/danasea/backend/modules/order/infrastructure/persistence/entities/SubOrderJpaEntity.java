package com.danasea.backend.modules.order.infrastructure.persistence.entities;

import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
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
@Table(name = "sub_orders")
public class SubOrderJpaEntity extends BaseJpaEntity {

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

    @Enumerated(EnumType.STRING)
    private SubOrderStatus status;

    private Boolean waiverAccepted;

    private OffsetDateTime waiverAcceptedAt;

    private UUID qrSecret;

    private OffsetDateTime checkedInAt;

}
