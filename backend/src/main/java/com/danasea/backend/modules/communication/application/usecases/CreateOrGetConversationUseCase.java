package com.danasea.backend.modules.communication.application.usecases;

import com.danasea.backend.modules.communication.application.dtos.ConversationResponse;
import com.danasea.backend.modules.communication.application.dtos.CreateConversationRequest;
import com.danasea.backend.modules.communication.application.dtos.CreateOrGetConversationResult;
import com.danasea.backend.modules.communication.application.dtos.MessageResponse;
import com.danasea.backend.modules.communication.application.mappers.CommunicationMapper;
import com.danasea.backend.modules.communication.domain.exceptions.UnauthorizedChatAccessException;
import com.danasea.backend.modules.communication.infrastructure.persistence.entities.ConversationJpaEntity;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaConversationRepository;
import com.danasea.backend.modules.communication.infrastructure.persistence.repositories.JpaMessageRepository;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrGetConversationUseCase {

    private final JpaConversationRepository conversationRepository;
    private final JpaMessageRepository messageRepository;
    private final JpaMasterOrderRepository masterOrderRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final VendorInternalApi vendorInternalApi;

    @Transactional
    public CreateOrGetConversationResult execute(CreateConversationRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedChatAccessException("User is not authenticated"));
        return execute(request, currentUserId);
    }

    @Transactional
    public CreateOrGetConversationResult execute(CreateConversationRequest request, UUID currentUserId) {
        if (request == null || request.masterOrderId() == null) {
            throw new IllegalArgumentException("Master order ID is required");
        }

        MasterOrderJpaEntity order = masterOrderRepository.findByIdForUpdate(request.masterOrderId())
                .orElseThrow(() -> new OrderNotFoundException(request.masterOrderId()));

        List<SubOrderJpaEntity> subOrders = subOrderRepository.findByMasterOrderId(order.getId());
        if (subOrders.isEmpty()) {
            throw new IllegalArgumentException("Order has no sub-orders");
        }

        UUID targetVendorId;
        boolean isCustomer = currentUserId.equals(order.getCustomerId());
        Optional<Vendor> vendorOpt = vendorInternalApi.findByUserId(currentUserId);

        if (isCustomer) {
            if (request.vendorId() != null) {
                boolean vendorInOrder = subOrders.stream()
                        .anyMatch(so -> request.vendorId().equals(so.getVendorId()));
                if (!vendorInOrder) {
                    throw new UnauthorizedChatAccessException("Specified vendor is not associated with this order");
                }
                targetVendorId = request.vendorId();
            } else {
                Set<UUID> distinctVendorIds = subOrders.stream()
                        .map(SubOrderJpaEntity::getVendorId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                if (distinctVendorIds.size() == 1) {
                    targetVendorId = distinctVendorIds.iterator().next();
                } else if (distinctVendorIds.isEmpty()) {
                    throw new IllegalArgumentException("No vendor found for this order");
                } else {
                    throw new IllegalArgumentException("Multiple vendors found in this order. Please specify vendorId");
                }
            }
        } else if (vendorOpt.isPresent()) {
            UUID vendorId = vendorOpt.get().getId();
            boolean vendorInOrder = subOrders.stream()
                    .anyMatch(so -> vendorId.equals(so.getVendorId()));
            if (!vendorInOrder) {
                throw new UnauthorizedChatAccessException("Vendor does not belong to this order");
            }
            if (request.vendorId() != null && !request.vendorId().equals(vendorId)) {
                throw new UnauthorizedChatAccessException("Vendor ID mismatch");
            }
            targetVendorId = vendorId;
        } else {
            throw new UnauthorizedChatAccessException("User is not authorized to access conversations for this order");
        }

        Optional<ConversationJpaEntity> existingOpt = conversationRepository
                .findByCustomerIdAndVendorIdAndMasterOrderId(order.getCustomerId(), targetVendorId, order.getId());

        if (existingOpt.isPresent()) {
            ConversationJpaEntity existing = existingOpt.get();
            MessageResponse lastMessage = messageRepository.findFirstByConversationIdOrderBySequenceDesc(existing.getId())
                    .map(CommunicationMapper::toMessageResponse)
                    .orElse(null);
            long unreadCount = messageRepository.countByConversationIdAndSenderIdNotAndIsReadFalse(existing.getId(), currentUserId);
            return new CreateOrGetConversationResult(
                    CommunicationMapper.toConversationResponse(existing, lastMessage, unreadCount),
                    false
            );
        }

        ConversationJpaEntity newConversation = ConversationJpaEntity.builder()
                .customerId(order.getCustomerId())
                .vendorId(targetVendorId)
                .masterOrderId(order.getId())
                .build();
        newConversation = conversationRepository.saveAndFlush(newConversation);

        ConversationResponse response = CommunicationMapper.toConversationResponse(newConversation, null, 0L);
        return new CreateOrGetConversationResult(response, true);
    }
}
