package com.danasea.backend.modules.ai.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_recommendation_feedback", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "idempotency_key"}))
@Getter
@Setter
public class AiRecommendationFeedbackJpaEntity extends BaseJpaEntity {
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false) private UUID recommendationId;
    @Column(nullable = false) private UUID serviceId;
    @Column(nullable = false, length = 16) private String signal;
    @Column(nullable = false, length = 120) private String idempotencyKey;
    @Column(nullable = false, length = 64) private String requestHash;
}
