package com.danasea.backend.modules.communication.infrastructure.persistence.entities;

import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.modules.communication.domain.models.NotificationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "notifications")
public class NotificationJpaEntity extends BaseJpaEntity {

    private UUID userId;

    private String type;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    private String title;

    private String body;

    private String relatedEntityType;

    private UUID relatedEntityId;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private OffsetDateTime sentAt;

}
