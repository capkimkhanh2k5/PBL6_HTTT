package com.danasea.backend.modules.ai.domain.models;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record AssistantArtifact(String id, String type, String tool, String status,
        Map<String, Object> facts, List<Source> sources, List<Action> actions,
        List<String> requiredInputs, boolean generatedTextVerified) {
    public record Source(String id, String kind, String reference, OffsetDateTime retrievedAt) { }
    public record Action(String type, String method, String path, Map<String, Object> parameters,
            boolean confirmationRequired) { }
}
