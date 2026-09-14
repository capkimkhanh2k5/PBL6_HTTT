package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AssistantAuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface JpaAssistantAuditLogRepository extends JpaRepository<AssistantAuditLogJpaEntity, UUID> {
    List<AssistantAuditLogJpaEntity> findByConversationId(UUID conversationId);
}
