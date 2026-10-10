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
@Table(name = "ai_customer_preferences")
@Getter
@Setter
public class AiCustomerPreferenceJpaEntity extends BaseJpaEntity {
    @Column(nullable = false, unique = true) private UUID ownerId;
    @Column(nullable = false) private boolean enabled;
    @Column(nullable = false, columnDefinition = "TEXT") private String interestsJson;
    @Column(nullable = false, columnDefinition = "TEXT") private String exclusionsJson;
    @Version private Long version;
}
