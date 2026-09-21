package com.danasea.backend.modules.communication.presentation.dtos;

import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.modules.communication.domain.models.NotificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private String type;
    private NotificationChannel channel;
    private String title;
    private String body;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private NotificationStatus status;
    private OffsetDateTime sentAt;
    private OffsetDateTime createdAt;
}
