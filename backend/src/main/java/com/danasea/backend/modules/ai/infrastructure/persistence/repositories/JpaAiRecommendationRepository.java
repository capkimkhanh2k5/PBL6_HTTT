package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiRecommendationJpaEntity;

public interface JpaAiRecommendationRepository extends JpaRepository<AiRecommendationJpaEntity, UUID> {
    Optional<AiRecommendationJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
