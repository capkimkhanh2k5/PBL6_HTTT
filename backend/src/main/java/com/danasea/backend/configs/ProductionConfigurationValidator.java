package com.danasea.backend.configs;

import java.util.List;

import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.danasea.backend.configs.properties.CloudinaryProperties;
import com.danasea.backend.configs.properties.CorsProperties;
import com.danasea.backend.configs.properties.PaymentProperties;
import com.danasea.backend.modules.ai.infrastructure.groq.config.GroqProperties;

import jakarta.annotation.PostConstruct;

@Component
@Profile("prod")
public class ProductionConfigurationValidator {

    private final CloudinaryProperties cloudinary;
    private final CorsProperties cors;
    private final GroqProperties groq;
    private final MailProperties mail;
    private final PaymentProperties payment;

    public ProductionConfigurationValidator(
            CloudinaryProperties cloudinary,
            CorsProperties cors,
            GroqProperties groq,
            MailProperties mail,
            PaymentProperties payment) {
        this.cloudinary = cloudinary;
        this.cors = cors;
        this.groq = groq;
        this.mail = mail;
        this.payment = payment;
    }

    @PostConstruct
    void validate() {
        requireText(cloudinary.cloudName(), "cloudinary.cloud-name");
        requireText(cloudinary.apiKey(), "cloudinary.api-key");
        requireText(cloudinary.apiSecret(), "cloudinary.api-secret");
        requireText(mail.getUsername(), "spring.mail.username");
        requireText(mail.getPassword(), "spring.mail.password");
        requireNonEmpty(groq.apiKeys(), "ai.groq.api-keys");
        requireNonEmpty(cors.allowedOrigins(), "app.cors.allowed-origins");
        requireText(payment.webhookSecret(), "app.payment.webhook-secret");

        if (cors.allowedOrigins().stream().anyMatch(this::isLocalhost)) {
            throw new IllegalStateException("Production CORS origins must not include localhost.");
        }
    }

    private void requireText(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required production configuration is missing: " + property);
        }
    }

    private void requireNonEmpty(List<String> values, String property) {
        if (values == null || values.stream().allMatch(value -> value == null || value.isBlank())) {
            throw new IllegalStateException("Required production configuration is missing: " + property);
        }
    }

    private boolean isLocalhost(String origin) {
        String normalized = origin == null ? "" : origin.toLowerCase();
        return normalized.contains("localhost") || normalized.contains("127.0.0.1");
    }
}
