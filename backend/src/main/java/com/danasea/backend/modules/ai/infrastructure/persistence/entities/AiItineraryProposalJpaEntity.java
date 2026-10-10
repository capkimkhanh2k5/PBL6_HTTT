package com.danasea.backend.modules.ai.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_itinerary_proposals")
@Getter
@Setter
public class AiItineraryProposalJpaEntity extends BaseJpaEntity {
    @Column(nullable = false) private UUID itineraryId;
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false) private long baseVersion;
    @Column(nullable = false, length = 20) private String state;
    @Column(nullable = false, columnDefinition = "TEXT") private String planJson;
    @Column(nullable = false, columnDefinition = "TEXT") private String originalPlanJson;
    @Column(nullable = false, length = 40) private String trigger;
    @Column(length = 200) private String sourceEventId;
}
