package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiRecommendationFeedbackJpaEntity;

public interface JpaAiRecommendationFeedbackRepository extends JpaRepository<AiRecommendationFeedbackJpaEntity, UUID> {
    Optional<AiRecommendationFeedbackJpaEntity> findByOwnerIdAndIdempotencyKey(UUID ownerId, String idempotencyKey);
    List<AiRecommendationFeedbackJpaEntity> findByOwnerIdAndSignalInOrderByCreatedAtDescIdDesc(UUID ownerId, List<String> signals, Pageable pageable);
}
