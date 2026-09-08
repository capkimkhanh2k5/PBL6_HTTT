package com.danasea.backend.modules.operation.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "settlement_items")
public class SettlementItemJpaEntity extends BaseJpaEntity {

    private UUID settlementId;

    private UUID subOrderId;

    private BigDecimal amount;

}
