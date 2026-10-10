package com.danasea.backend.modules.communication.application.usecases;

import com.danasea.backend.modules.communication.application.dtos.MessageResponse;
import com.danasea.backend.modules.communication.application.dtos.SendMessageRequest;
import com.danasea.backend.modules.communication.application.mappers.CommunicationMapper;
import com.danasea.backend.modules.communication.domain.exceptions.ConversationNotFoundException;
import com.danasea.backend.modules.communication.domain.exceptions.UnauthorizedChatAccessException;
import com.danasea.backend.modules.communication.infrastructure.persistence.entities.ConversationJpaEntity;
import com.danasea.backend.modules.communication.infrastructure.persistence.entities.MessageJpaEntity;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaConversationRepository;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaMessageRepository;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendMessageUseCase {

    private final JpaConversationRepository conversationRepository;
    private final JpaMessageRepository messageRepository;
    private final VendorInternalApi vendorInternalApi;

    @Transactional
    public MessageResponse execute(UUID conversationId, SendMessageRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedChatAccessException("User is not authenticated"));
        return execute(conversationId, request, currentUserId);
    }

    @Transactional
    public MessageResponse execute(UUID conversationId, SendMessageRequest request, UUID currentUserId) {
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }

        ConversationJpaEntity conv = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        boolean isCustomer = conv.getCustomerId().equals(currentUserId);
        boolean isVendor = false;
        Optional<Vendor> vendorOpt = vendorInternalApi.findByUserId(currentUserId);
        if (vendorOpt.isPresent() && conv.getVendorId().equals(vendorOpt.get().getId())) {
            isVendor = true;
        }

        if (!isCustomer && !isVendor) {
            throw new UnauthorizedChatAccessException("User is not authorized to send messages in this conversation");
        }

        long sequence = Math.addExact(conv.getLastMessageSequence(), 1L);
        conv.setLastMessageSequence(sequence);
        MessageJpaEntity message = MessageJpaEntity.builder()
                .sequence(sequence)
                .conversationId(conv.getId())
                .senderId(currentUserId)
                .content(request.content().trim())
                .attachmentUrl(request.attachmentUrl())
                .isRead(false)
                .build();
        message = messageRepository.saveAndFlush(message);

        conv.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conv);

        return CommunicationMapper.toMessageResponse(message);
    }
}
