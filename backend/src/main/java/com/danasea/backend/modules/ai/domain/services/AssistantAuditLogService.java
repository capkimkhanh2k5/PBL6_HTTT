package com.danasea.backend.modules.ai.domain.services;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AssistantAuditLogJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAssistantAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssistantAuditLogService {

    private final JpaAssistantAuditLogRepository auditLogRepository;

    @Transactional
    public void logToolCall(UUID conversationId, UUID userId, String toolName, String requestPayload, String responsePayload) {
        AssistantAuditLogJpaEntity log = AssistantAuditLogJpaEntity.builder()
                .conversationId(conversationId)
                .userId(userId)
                .toolName(toolName)
                .requestPayload(requestPayload)
                .responsePayload(responsePayload)
                .executedAt(OffsetDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    @Transactional
    public void logChatAction(UUID conversationId, UUID userId, String requestMessage, String responseMessage, String keyMasked) {
        AssistantAuditLogJpaEntity log = AssistantAuditLogJpaEntity.builder()
                .conversationId(conversationId)
                .userId(userId)
                .toolName("chat")
                .requestPayload(requestMessage)
                .responsePayload(responseMessage)
                .executedAt(OffsetDateTime.now())
                .keyMasked(keyMasked)
                .build();
        auditLogRepository.save(log);
    }
}
