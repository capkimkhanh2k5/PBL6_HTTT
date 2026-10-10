package com.danasea.backend.modules.communication.application.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String content,
        String attachmentUrl,
        Boolean isRead,
        OffsetDateTime createdAt,
        Long sequence
) {
    public MessageResponse(UUID id, UUID conversationId, UUID senderId, String content,
            String attachmentUrl, Boolean isRead, OffsetDateTime createdAt) {
        this(id, conversationId, senderId, content, attachmentUrl, isRead, createdAt, null);
    }
}
