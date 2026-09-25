package com.danasea.backend.modules.ai.infrastructure.groq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "ai.groq")
public record GroqProperties(
        @NotBlank String baseUrl,
        List<String> apiKeys,
        @NotBlank String model,
        @NotBlank String fallbackModel,
        @NotBlank String moderationModel
) {
    public GroqProperties {
        if (apiKeys == null) {
            apiKeys = List.of();
        }
    }
}
