package com.danasea.backend.modules.ai.domain.services;

import com.danasea.backend.modules.ai.domain.models.AiMessageRole;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiConversationJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiMessageJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiConversationRepository;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final JpaAiConversationRepository conversationRepository;
    private final JpaAiMessageRepository messageRepository;

    @Transactional
    public AiConversationJpaEntity getOrCreateConversation(UUID conversationId, UUID userId) {
        if (conversationId != null) {
            return conversationRepository.findById(conversationId)
                    .map(conversation -> requireOwnership(conversation, userId))
                    .orElseGet(() -> createNewConversation(userId));
        }
        return createNewConversation(userId);
    }

    @Transactional(readOnly = true)
    public Optional<AiConversationJpaEntity> getConversationForUser(UUID conversationId, UUID userId) {
        return conversationRepository.findById(conversationId)
                .map(conversation -> requireOwnership(conversation, userId));
    }

    private AiConversationJpaEntity createNewConversation(UUID userId) {
        AiConversationJpaEntity conversation = new AiConversationJpaEntity();
        conversation.setUserId(userId);
        conversation.setStartedAt(OffsetDateTime.now());
        return conversationRepository.save(conversation);
    }

    @Transactional
    public AiMessageJpaEntity appendMessage(UUID conversationId, AiMessageRole role, String content, String toolCalls) {
        AiMessageJpaEntity message = new AiMessageJpaEntity();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setToolCalls(toolCalls);
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<AiMessageJpaEntity> getRecentMessages(UUID conversationId, int limit) {
        // Fetch the last N messages for the LLM Context window
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId,
                PageRequest.of(0, limit, Sort.by("createdAt").descending())
        );
    }

    @Transactional(readOnly = true)
    public Optional<List<AiMessageJpaEntity>> getRecentMessagesForUser(
            UUID conversationId,
            UUID userId,
            int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        return getConversationForUser(conversationId, userId)
                .map(conversation -> getRecentMessages(conversation.getId(), boundedLimit));
    }

    private AiConversationJpaEntity requireOwnership(AiConversationJpaEntity conversation, UUID userId) {
        if (userId == null || !userId.equals(conversation.getUserId())) {
            throw new AccessDeniedException("Conversation does not belong to the authenticated user");
        }
        return conversation;
    }
}
