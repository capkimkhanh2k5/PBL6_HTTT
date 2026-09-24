package com.danasea.backend.modules.ai.presentation.dtos;

import java.util.UUID;

public record ChatResponse(
        String status,
        UUID conversationId,
        String message
) {
}
