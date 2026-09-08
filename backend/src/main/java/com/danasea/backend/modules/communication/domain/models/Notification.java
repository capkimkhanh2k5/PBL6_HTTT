package com.danasea.backend.modules.communication.domain.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseDomainModel {
    private UUID userId;
    private String type;
    private NotificationChannel channel;
    private String title;
    private String body;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private NotificationStatus status;
    private OffsetDateTime sentAt;
}
