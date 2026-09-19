package com.danasea.backend.modules.ai.domain.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {
    private String id;
    private String name;
    private String arguments;
}
