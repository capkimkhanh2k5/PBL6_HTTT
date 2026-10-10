package com.danasea.backend.modules.communication.application.dtos;

public record CreateOrGetConversationResult(
        ConversationResponse conversation,
        boolean isNew
) {
}
