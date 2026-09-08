package com.danasea.backend.modules.ai.infrastructure.persistence.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "ai_conversations")
public class AiConversationJpaEntity extends BaseJpaEntity {

    private UUID userId;

    private OffsetDateTime startedAt;

    private OffsetDateTime endedAt;

}
