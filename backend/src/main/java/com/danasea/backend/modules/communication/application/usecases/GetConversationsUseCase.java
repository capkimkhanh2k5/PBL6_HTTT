package com.danasea.backend.modules.communication.application.usecases;

import com.danasea.backend.modules.communication.application.dtos.ConversationResponse;
import com.danasea.backend.modules.communication.application.dtos.MessageResponse;
import com.danasea.backend.modules.communication.application.mappers.CommunicationMapper;
import com.danasea.backend.modules.communication.domain.exceptions.UnauthorizedChatAccessException;
import com.danasea.backend.modules.communication.infrastructure.persistence.entities.ConversationJpaEntity;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaConversationRepository;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaMessageRepository;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetConversationsUseCase {

    private final JpaConversationRepository conversationRepository;
    private final JpaMessageRepository messageRepository;
    private final VendorInternalApi vendorInternalApi;

    @Transactional(readOnly = true)
    public Page<ConversationResponse> execute(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedChatAccessException("User is not authenticated"));
        return execute(currentUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> execute(UUID currentUserId, Pageable pageable) {
        if (pageable == null || !pageable.isPaged() || pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        Optional<Vendor> vendorOpt = vendorInternalApi.findByUserId(currentUserId);
        Page<ConversationJpaEntity> page;
        if (vendorOpt.isPresent()) {
            UUID vendorId = vendorOpt.get().getId();
            page = conversationRepository.findByCustomerIdOrVendorId(currentUserId, vendorId, pageable);
        } else {
            page = conversationRepository.findByCustomerId(currentUserId, pageable);
        }

        return page.map(conv -> {
            MessageResponse lastMessage = messageRepository.findFirstByConversationIdOrderBySequenceDesc(conv.getId())
                    .map(CommunicationMapper::toMessageResponse)
                    .orElse(null);
            long unreadCount = messageRepository.countByConversationIdAndSenderIdNotAndIsReadFalse(conv.getId(), currentUserId);
            return CommunicationMapper.toConversationResponse(conv, lastMessage, unreadCount);
        });
    }
}
