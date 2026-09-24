package com.danasea.backend.modules.communication.domain.events;

import java.util.UUID;

public record NotificationEmailEvent(
        UUID notificationId,
        String toEmail,
        String subject,
        String content,
        String locale
) {
    public NotificationEmailEvent(UUID notificationId, String toEmail, String subject, String content) {
        this(notificationId, toEmail, subject, content, "vi");
    }
}
