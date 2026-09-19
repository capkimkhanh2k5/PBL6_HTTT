package com.danasea.backend.modules.ai.domain.models;

import lombok.Data;
import java.util.List;

@Data
public class LlmResponse {
    private String content;
    private List<ToolCall> toolCalls;
    private String keyMasked;
}
