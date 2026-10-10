package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiChatRequestJpaEntity;

public interface JpaAiChatRequestRepository extends JpaRepository<AiChatRequestJpaEntity, UUID> {
    Optional<AiChatRequestJpaEntity> findByActorIdAndRequestKey(UUID actorId, String requestKey);
}
