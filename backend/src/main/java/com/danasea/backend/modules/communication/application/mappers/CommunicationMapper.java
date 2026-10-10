package com.danasea.backend.modules.communication.application.mappers;

import com.danasea.backend.modules.communication.application.dtos.ConversationResponse;
import com.danasea.backend.modules.communication.application.dtos.MessageResponse;
import com.danasea.backend.modules.communication.infrastructure.persistence.entities.ConversationJpaEntity;
import com.danasea.backend.modules.communication.infrastructure.persistence.entities.MessageJpaEntity;

public final class CommunicationMapper {

    private CommunicationMapper() {
    }

    public static MessageResponse toMessageResponse(MessageJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MessageResponse(
                entity.getId(),
                entity.getConversationId(),
                entity.getSenderId(),
                entity.getContent(),
                entity.getAttachmentUrl(),
                Boolean.TRUE.equals(entity.getIsRead()),
                entity.getCreatedAt(),
                entity.getSequence()
        );
    }

    public static ConversationResponse toConversationResponse(
            ConversationJpaEntity entity,
            MessageResponse lastMessage,
            long unreadCount) {
        if (entity == null) {
            return null;
        }
        return new ConversationResponse(
                entity.getId(),
                entity.getMasterOrderId(),
                entity.getCustomerId(),
                entity.getVendorId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                lastMessage,
                unreadCount
        );
    }
}
