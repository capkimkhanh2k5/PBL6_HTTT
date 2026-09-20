package com.danasea.backend.modules.communication.domain.events;

import java.util.UUID;

public record NotificationEmailEvent(
        UUID notificationId,
        String toEmail,
        String subject,
        String content
) {}
