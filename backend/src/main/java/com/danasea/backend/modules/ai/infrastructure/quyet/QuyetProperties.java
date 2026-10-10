package com.danasea.backend.modules.ai.infrastructure.quyet;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ai.quyet")
public record QuyetProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("http://127.0.0.1:8091") String baseUrl,
        @DefaultValue("") String apiKey,
        @DefaultValue("1s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue("233167bba5df61b5375a522bf8a042d8d2189379") String revision) {}
