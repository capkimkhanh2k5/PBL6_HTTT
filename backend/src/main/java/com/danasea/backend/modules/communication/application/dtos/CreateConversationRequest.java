package com.danasea.backend.modules.communication.application.dtos;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateConversationRequest(
        @NotNull(message = "{validation.communication.master_order.required}")
        UUID masterOrderId,
        UUID vendorId
) {
    public CreateConversationRequest(UUID masterOrderId) {
        this(masterOrderId, null);
    }
}
