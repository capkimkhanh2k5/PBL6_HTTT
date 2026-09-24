package com.danasea.backend.modules.ai.application.tool;

import java.util.UUID;

import com.danasea.backend.shared.i18n.SupportedLanguage;

public record ToolExecutionContext(SupportedLanguage language, UUID conversationId) {}
