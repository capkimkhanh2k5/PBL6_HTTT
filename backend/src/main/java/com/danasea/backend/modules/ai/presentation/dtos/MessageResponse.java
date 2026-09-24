package com.danasea.backend.modules.ai.presentation.dtos;

import com.danasea.backend.modules.ai.domain.models.AiMessageRole;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiMessageJpaEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        AiMessageRole role,
        String content,
        String toolCalls,
        OffsetDateTime createdAt
) {
    public static MessageResponse from(AiMessageJpaEntity message) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getRole(),
                message.getContent(),
                message.getToolCalls(),
                message.getCreatedAt());
    }
}
