package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiConversationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaAiConversationRepository extends JpaRepository<AiConversationJpaEntity, UUID> {
}
