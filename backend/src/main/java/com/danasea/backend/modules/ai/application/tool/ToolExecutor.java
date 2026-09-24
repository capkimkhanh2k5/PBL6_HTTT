package com.danasea.backend.modules.ai.application.tool;

public interface ToolExecutor {
    String getName();
    String execute(String argumentsJson);

    default String execute(String argumentsJson, ToolExecutionContext context) {
        return execute(argumentsJson);
    }
}
