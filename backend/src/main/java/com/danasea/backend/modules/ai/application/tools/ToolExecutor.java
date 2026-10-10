package com.danasea.backend.modules.ai.application.tools;

public interface ToolExecutor {
    String getName();
    String execute(String argumentsJson);

    default String execute(String argumentsJson, ToolExecutionContext context) {
        return execute(argumentsJson);
    }
}
