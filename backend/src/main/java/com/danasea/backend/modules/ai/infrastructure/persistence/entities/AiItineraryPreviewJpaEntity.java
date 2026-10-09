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
@Table(name = "ai_itinerary_previews")
@Getter
@Setter
public class AiItineraryPreviewJpaEntity extends BaseJpaEntity {
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false, length = 5) private String locale = "vi";
    @Column(nullable = false, columnDefinition = "TEXT") private String alternativesJson;
    @Column(nullable = false) private OffsetDateTime expiresAt;
}
