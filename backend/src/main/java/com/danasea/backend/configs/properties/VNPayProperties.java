package com.danasea.backend.configs.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Cấu hình tham số cổng thanh toán VNPay.
 * Sử dụng trong VNPayPaymentAdapter để khởi tạo giao dịch VNPay-QR và tính mã băm HMAC-SHA512 checksum.
 */
@Validated
@ConfigurationProperties(prefix = "app.payment.vnpay")
public record VNPayProperties(
        @NotBlank String tmnCode,
        @NotBlank String hashSecret,
        @NotBlank String payUrl,
        String returnUrl
) {
    public VNPayProperties {
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = "http://localhost:3000/payment/vnpay-return";
        }
    }
}
