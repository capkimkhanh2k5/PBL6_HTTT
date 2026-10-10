package com.danasea.backend.modules.communication.application.usecases;

import com.danasea.backend.modules.communication.domain.exceptions.ConversationNotFoundException;
import com.danasea.backend.modules.communication.domain.exceptions.UnauthorizedChatAccessException;
import com.danasea.backend.modules.communication.infrastructure.persistence.entities.ConversationJpaEntity;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaConversationRepository;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaMessageRepository;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarkMessagesAsReadUseCase {

    private final JpaConversationRepository conversationRepository;
    private final JpaMessageRepository messageRepository;
    private final VendorInternalApi vendorInternalApi;

    @Transactional
    public void execute(UUID conversationId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedChatAccessException("User is not authenticated"));
        execute(conversationId, currentUserId);
    }

    @Transactional
    public void execute(UUID conversationId, UUID currentUserId) {
        verifyParticipant(conversationId, currentUserId);
        int updated = messageRepository.markMessagesAsRead(conversationId, currentUserId);
        log.debug("Marked {} messages as read in conversation {} for user {}", updated, conversationId, currentUserId);
    }

    @Transactional
    public void execute(UUID conversationId, UUID currentUserId, Collection<UUID> messageIds) {
        verifyParticipant(conversationId, currentUserId);
        if (!messageIds.isEmpty()) {
            messageRepository.markDeliveredMessagesAsRead(conversationId, currentUserId, messageIds);
        }
    }

    private void verifyParticipant(UUID conversationId, UUID currentUserId) {
        ConversationJpaEntity conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        boolean isCustomer = conv.getCustomerId().equals(currentUserId);
        boolean isVendor = false;
        Optional<Vendor> vendorOpt = vendorInternalApi.findByUserId(currentUserId);
        if (vendorOpt.isPresent() && conv.getVendorId().equals(vendorOpt.get().getId())) {
            isVendor = true;
        }

        if (!isCustomer && !isVendor) {
            throw new UnauthorizedChatAccessException("User is not a participant of this conversation");
        }
    }
}
