package com.danasea.backend.modules.ai.domain.services;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiConversationJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiConversationRepository;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHistoryOwnershipTest {

    @Mock
    private JpaAiConversationRepository conversationRepository;

    @Mock
    private JpaAiMessageRepository messageRepository;

    private ChatHistoryService service;

    @BeforeEach
    void setUp() {
        service = new ChatHistoryService(conversationRepository, messageRepository);
    }

    @Test
    void getOrCreateConversation_rejectsConversationOwnedByAnotherUser() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        AiConversationJpaEntity conversation = conversation(conversationId, ownerId);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> service.getOrCreateConversation(conversationId, attackerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getConversationForUser_rejectsConversationOwnedByAnotherUser() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        AiConversationJpaEntity conversation = conversation(conversationId, ownerId);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> service.getConversationForUser(conversationId, attackerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getRecentMessagesForUser_rejectsConversationOwnedByAnotherUser() {
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        AiConversationJpaEntity conversation = conversation(conversationId, ownerId);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> service.getRecentMessagesForUser(conversationId, attackerId, 20))
                .isInstanceOf(AccessDeniedException.class);
    }

    private AiConversationJpaEntity conversation(UUID conversationId, UUID userId) {
        AiConversationJpaEntity conversation = new AiConversationJpaEntity();
        conversation.setId(conversationId);
        conversation.setUserId(userId);
        return conversation;
    }
}
