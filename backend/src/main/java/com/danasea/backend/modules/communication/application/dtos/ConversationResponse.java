package com.danasea.backend.modules.communication.application.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID masterOrderId,
        UUID customerId,
        UUID vendorId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        MessageResponse lastMessage,
        long unreadCount
) {
    public ConversationResponse(
            UUID id,
            UUID masterOrderId,
            UUID customerId,
            UUID vendorId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            MessageResponse lastMessage) {
        this(id, masterOrderId, customerId, vendorId, createdAt, updatedAt, lastMessage, 0L);
    }
}
