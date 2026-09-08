package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiMessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAiMessageRepository extends JpaRepository<AiMessageJpaEntity, UUID> {
}
