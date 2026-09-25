package com.danasea.backend.modules.ai.application.tool;

import java.util.UUID;

public interface ConversationAwareToolExecutor extends ToolExecutor {
    String execute(String argumentsJson, UUID conversationId);
}
