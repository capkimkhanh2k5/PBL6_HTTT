package com.danasea.backend.configs.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @Valid @NotNull Limit loginRegistration,
        @Valid @NotNull Limit otp) {

    public record Limit(
            @Min(1) long capacity,
            @NotNull Duration refillPeriod) {

        public Limit {
            if (refillPeriod != null && (refillPeriod.isZero() || refillPeriod.isNegative())) {
                throw new IllegalArgumentException("Rate-limit refill period must be positive.");
            }
        }
    }
}
