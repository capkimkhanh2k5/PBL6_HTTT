package com.danasea.backend.configs.properties;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "open-meteo")
public record OpenMeteoProperties(
        @NotNull URI baseUrl,
        @NotNull URI marineUrl) {
}
