package com.danasea.backend.configs.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Cấu hình tham số cổng thanh toán PayPal REST API v2.
 * Sử dụng trong PayPalPaymentAdapter để xác thực OAuth2 Bearer token, tạo đơn hàng, hoàn tiền và xác thực webhook.
 */
@Validated
@ConfigurationProperties(prefix = "app.payment.paypal")
public record PayPalProperties(
        @NotBlank String mode,
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        String webhookId,
        @NotBlank String baseUrl,
        String returnUrl,
        String cancelUrl
) {
    public PayPalProperties {
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = "http://localhost:3000/payment/success";
        }
        if (cancelUrl == null || cancelUrl.isBlank()) {
            cancelUrl = "http://localhost:3000/payment/cancel";
        }
    }
}
