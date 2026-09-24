package com.danasea.backend.modules.communication.application.dtos;

import java.util.UUID;

import com.danasea.backend.modules.communication.domain.models.NotificationChannel;
import com.danasea.backend.shared.i18n.LocalizedMessageRef;

public record NotificationCommand(
        UUID recipientId,
        String type,
        NotificationChannel channel,
        LocalizedMessageRef title,
        LocalizedMessageRef body,
        String relatedEntityType,
        UUID relatedEntityId,
        String recipientEmail
) {}
