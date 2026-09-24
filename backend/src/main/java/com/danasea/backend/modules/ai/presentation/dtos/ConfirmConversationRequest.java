package com.danasea.backend.modules.ai.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmConversationRequest(
        @NotBlank @Size(max = 128) String cardId,
        @Size(max = 128) String sessionId
) {
}
