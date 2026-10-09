package com.danasea.backend.modules.ai.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_itineraries")
@Getter
@Setter
public class AiItineraryJpaEntity extends BaseJpaEntity {
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false, columnDefinition = "TEXT") private String planJson;
    @Version private Long version;
}
