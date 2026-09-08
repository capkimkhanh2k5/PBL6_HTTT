package com.danasea.backend.modules.ai.domain.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiConversation extends BaseDomainModel {
    private UUID userId;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
}
