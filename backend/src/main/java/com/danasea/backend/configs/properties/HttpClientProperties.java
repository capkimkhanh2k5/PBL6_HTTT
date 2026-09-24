package com.danasea.backend.configs.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.http")
public record HttpClientProperties(
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {

    public HttpClientProperties {
        requirePositive(connectTimeout, "connect timeout");
        requirePositive(readTimeout, "read timeout");
    }

    private static void requirePositive(Duration value, String name) {
        if (value != null && (value.isZero() || value.isNegative())) {
            throw new IllegalArgumentException("HTTP " + name + " must be positive.");
        }
    }
}
