package com.danasea.backend.modules.communication.infrastructure.persistence.entities;

import com.danasea.backend.modules.communication.domain.models.DisputeStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "disputes")
public class DisputeJpaEntity extends BaseJpaEntity {

    private UUID subOrderId;

    private UUID raisedBy;

    private String category;

    private String description;

    @Enumerated(EnumType.STRING)
    private DisputeStatus status;

    private String resolutionNote;

    private UUID resolvedBy;

    private OffsetDateTime resolvedAt;

}
