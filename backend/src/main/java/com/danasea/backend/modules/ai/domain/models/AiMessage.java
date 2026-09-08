package com.danasea.backend.modules.ai.domain.models;

import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiMessage extends BaseDomainModel {
    private UUID conversationId;
    private AiMessageRole role;
    private String content;
    private String toolCalls;
}
