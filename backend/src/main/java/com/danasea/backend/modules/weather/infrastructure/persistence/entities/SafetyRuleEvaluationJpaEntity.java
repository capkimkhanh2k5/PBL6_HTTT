package com.danasea.backend.modules.weather.infrastructure.persistence.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "safety_rule_evaluations")
public class SafetyRuleEvaluationJpaEntity extends BaseJpaEntity {

    private UUID serviceId;

    private UUID slotId;

    private Boolean isSafe;

    private String warningMessage;

    private OffsetDateTime evaluatedAt;

}
