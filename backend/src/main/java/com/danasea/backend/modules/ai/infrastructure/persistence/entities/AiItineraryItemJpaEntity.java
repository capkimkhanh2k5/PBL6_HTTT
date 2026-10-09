package com.danasea.backend.modules.ai.infrastructure.persistence.entities;

import java.time.LocalDate;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_itinerary_items")
@Getter
@Setter
public class AiItineraryItemJpaEntity extends BaseJpaEntity {
    @Column(nullable = false) private UUID itineraryId;
    @Column(nullable = false) private UUID slotId;
    @Column(nullable = false) private UUID serviceId;
    @Column(nullable = false) private LocalDate activityDate;
}
