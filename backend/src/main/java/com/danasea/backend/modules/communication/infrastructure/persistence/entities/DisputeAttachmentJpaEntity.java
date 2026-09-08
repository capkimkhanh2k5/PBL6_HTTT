package com.danasea.backend.modules.communication.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "dispute_attachments")
public class DisputeAttachmentJpaEntity extends BaseJpaEntity {

    private UUID disputeId;

    private String fileUrl;

}
