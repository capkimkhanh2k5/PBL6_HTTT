package com.danasea.backend.modules.ai.infrastructure.persistence.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_recommendations")
@Getter
@Setter
public class AiRecommendationJpaEntity extends BaseJpaEntity {
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false, columnDefinition = "TEXT") private String serviceIdsJson;
    @Column(nullable = false, length = 128) private String criteriaFingerprint;
    @Column(nullable = false) private OffsetDateTime expiresAt;
}
