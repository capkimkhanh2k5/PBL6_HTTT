package com.danasea.backend.configs.properties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app.checkin")
public record CheckinProperties(
        @NotBlank String qrSecret,
        Duration opensBefore,
        Duration closesAfter) {

    public CheckinProperties {
        if (qrSecret != null && qrSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("The check-in QR signing secret must contain at least 256 bits.");
        }
        opensBefore = opensBefore == null ? Duration.ofHours(2) : opensBefore;
        closesAfter = closesAfter == null ? Duration.ofMinutes(30) : closesAfter;
        if (opensBefore.isNegative() || closesAfter.isNegative()) {
            throw new IllegalArgumentException("Check-in window durations must not be negative.");
        }
    }
}
