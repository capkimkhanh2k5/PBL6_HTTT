package com.danasea.backend.modules.ai.infrastructure.groq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ai.groq")
public record GroqProperties(
        String baseUrl,
        List<String> apiKeys,
        String model,
        String fallbackModel,
        String moderationModel
) {
    public GroqProperties {
        if (apiKeys == null) {
            apiKeys = List.of();
        }
    }
}
