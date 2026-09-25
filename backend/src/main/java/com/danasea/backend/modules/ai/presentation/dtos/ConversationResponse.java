package com.danasea.backend.modules.ai.presentation.dtos;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiConversationJpaEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ConversationResponse from(AiConversationJpaEntity conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getStartedAt(),
                conversation.getEndedAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }
}
