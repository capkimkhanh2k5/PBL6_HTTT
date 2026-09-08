package com.danasea.backend.modules.communication.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "conversations")
public class ConversationJpaEntity extends BaseJpaEntity {

    private UUID customerId;

    private UUID vendorId;

    private UUID masterOrderId;

}
