package com.danasea.backend.modules.ai.infrastructure.persistence.entities;

import com.danasea.backend.modules.ai.domain.models.AiMessageRole;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "ai_messages")
public class AiMessageJpaEntity extends BaseJpaEntity {

    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    private AiMessageRole role;

    private String content;

    private String toolCalls;

}
