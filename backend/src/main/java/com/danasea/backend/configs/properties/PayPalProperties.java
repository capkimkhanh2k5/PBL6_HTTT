package com.danasea.backend.configs.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment.paypal")
public record PayPalProperties(
        String mode,
        String clientId,
        String clientSecret,
        String webhookId,
        String baseUrl,
        String returnUrl,
        String cancelUrl
) {
    public PayPalProperties {
        if (mode == null || mode.isBlank()) {
            mode = "sandbox";
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "sandbox".equalsIgnoreCase(mode)
                    ? "https://api-m.sandbox.paypal.com"
                    : "https://api-m.paypal.com";
        }
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = "http://localhost:3000/payment/success";
        }
        if (cancelUrl == null || cancelUrl.isBlank()) {
            cancelUrl = "http://localhost:3000/payment/cancel";
        }
    }
}
