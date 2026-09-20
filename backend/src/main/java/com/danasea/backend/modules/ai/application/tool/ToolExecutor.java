package com.danasea.backend.modules.ai.application.tool;

public interface ToolExecutor {
    String getName();
    String execute(String argumentsJson);
}
