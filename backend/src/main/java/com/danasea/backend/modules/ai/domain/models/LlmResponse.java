package com.danasea.backend.modules.ai.domain.models;

import lombok.Data;
import java.util.List;

@Data
public class LlmResponse {
    private String content;
    private List<ToolCall> toolCalls;
    private String keyMasked;
    private List<AssistantArtifact> cards = List.of();
    private List<String> requiredInputs = List.of();
    private String responseStatus = "ANSWERED";
    private java.util.Map<String, Object> conversationContext = java.util.Map.of();
    private boolean generatedTextVerified;
}
