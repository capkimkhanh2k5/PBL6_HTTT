package com.danasea.backend.configs.properties;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(String webhookSecret) {

    public PaymentProperties {
        if (webhookSecret == null || webhookSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("The payment webhook secret must contain at least 256 bits.");
        }
    }
}
