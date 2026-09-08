package com.danasea.backend.modules.weather.domain.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SafetyRuleEvaluation extends BaseDomainModel {
    private UUID serviceId;
    private UUID slotId;
    private Boolean isSafe;
    private String warningMessage;
    private OffsetDateTime evaluatedAt;
}
