package com.danasea.backend.configs.properties;

import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.scheduler")
public record SchedulerProperties(
        @NotNull ZoneId zone,
        @Min(1) int poolSize,
        @NotNull Duration bookingCleanupDelay,
        @NotNull Duration weatherFetchRate) {
}
