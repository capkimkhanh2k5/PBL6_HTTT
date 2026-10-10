package com.danasea.backend.modules.ai.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_itinerary_revisions")
@Getter
@Setter
public class AiItineraryRevisionJpaEntity extends BaseJpaEntity {
    @Column(nullable = false) private UUID itineraryId;
    @Column(nullable = false) private long itineraryVersion;
    @Column(nullable = false, length = 20) private String lifecycle;
    @Column(nullable = false, columnDefinition = "TEXT") private String planJson;
    @Column(nullable = false, length = 40) private String reason;
    private UUID proposalId;
}
