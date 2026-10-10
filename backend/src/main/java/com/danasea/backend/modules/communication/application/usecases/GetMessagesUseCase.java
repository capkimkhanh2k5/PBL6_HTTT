package com.danasea.backend.modules.communication.application.usecases;

import com.danasea.backend.modules.communication.application.dtos.MessageResponse;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetMessagesUseCase {

    private final JpaConversationRepository conversationRepository;
    private final JpaMessageRepository messageRepository;
    private final VendorInternalApi vendorInternalApi;
    private final MarkMessagesAsReadUseCase markMessagesAsReadUseCase;

    @Transactional
    public List<MessageResponse> execute(UUID conversationId, OffsetDateTime after, Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedChatAccessException("User is not authenticated"));
        return execute(conversationId, after, pageable, currentUserId);
    }

    @Transactional
    public List<MessageResponse> execute(UUID conversationId, OffsetDateTime after, Pageable pageable, UUID currentUserId) {
        return execute(conversationId, after, null, pageable, currentUserId);
    }

    @Transactional
    public List<MessageResponse> execute(UUID conversationId, OffsetDateTime after, Long afterSequence,
            Pageable pageable, UUID currentUserId) {
        if (after != null && afterSequence != null) {
            throw new IllegalArgumentException("Specify either after or afterSequence");
        }
        if (afterSequence != null && afterSequence < 0) {
            throw new IllegalArgumentException("afterSequence must be non-negative");
        }
        int size = pageable != null && pageable.isPaged() ? pageable.getPageSize() : 50;
        if (size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        ConversationJpaEntity conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        boolean isCustomer = conv.getCustomerId().equals(currentUserId);
        boolean isVendor = false;
        Optional<Vendor> vendorOpt = vendorInternalApi.findByUserId(currentUserId);
        if (vendorOpt.isPresent() && conv.getVendorId().equals(vendorOpt.get().getId())) {
            isVendor = true;
        }

        if (!isCustomer && !isVendor) {
            throw new UnauthorizedChatAccessException("User does not have access to this conversation");
        }

        List<MessageJpaEntity> messages;
        if (afterSequence != null) {
            messages = messageRepository.findByConversationIdAndSequenceGreaterThanOrderBySequenceAsc(
                    conversationId, afterSequence, PageRequest.of(0, size));
        } else if (after != null) {
            Optional<Long> sequence = messageRepository.findFirstSequenceAtTimestamp(conversationId, after);
            messages = sequence.isPresent()
                    ? messageRepository.findByConversationIdAndSequenceGreaterThanOrderBySequenceAsc(
                            conversationId, sequence.get(), PageRequest.of(0, size))
                    : messageRepository.findByConversationIdAndCreatedAtGreaterThanOrderBySequenceAsc(
                            conversationId, after, PageRequest.of(0, size));
        } else {
            int page = pageable != null && pageable.isPaged() ? pageable.getPageNumber() : 0;
            messages = messageRepository.findByConversationId(conversationId,
                    PageRequest.of(page, size, Sort.by("sequence").ascending())).getContent();
        }

        List<UUID> deliveredIds = messages.stream().map(MessageJpaEntity::getId).toList();
        markMessagesAsReadUseCase.execute(conversationId, currentUserId, deliveredIds);
        messages.stream().filter(msg -> !currentUserId.equals(msg.getSenderId()))
                .forEach(msg -> msg.setIsRead(true));
        return messages.stream().map(CommunicationMapper::toMessageResponse).toList();
    }
}
