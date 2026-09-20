package com.danasea.backend.modules.ai.infrastructure.groq.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GroqProperties.class)
public class AiConfig {
}
