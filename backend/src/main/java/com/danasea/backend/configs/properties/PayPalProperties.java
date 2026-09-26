package com.danasea.backend.configs.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình tham số cổng thanh toán PayPal REST API v2 Sandbox.
 * <p>
 * Luồng cấu hình (Traceability Map):
 * <ul>
 *   <li>Biến môi trường: {@code APP_PAYPAL_MODE}, {@code APP_PAYPAL_CLIENT_ID}, {@code APP_PAYPAL_CLIENT_SECRET}, {@code APP_PAYPAL_WEBHOOK_ID} (trong {@code .env})</li>
 *   <li>File cấu hình: {@code backend/src/main/resources/application.yml} (mục {@code app.payment.paypal})</li>
 *   <li>Sử dụng tại: {@code PayPalPaymentAdapter.java} để xác thực OAuth2 Bearer token, tạo đơn hàng v2/checkout/orders, hoàn tiền và xác thực webhook</li>
 * </ul>
 */
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
