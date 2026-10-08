package com.danasea.backend.configs.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
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
        String returnUrl,
        String apiUrl,
        String ipAddress
) {
    public VNPayProperties(String tmnCode, String hashSecret, String payUrl, String returnUrl, String apiUrl) {
        this(tmnCode, hashSecret, payUrl, returnUrl, apiUrl, "127.0.0.1");
    }

    @ConstructorBinding
    public VNPayProperties {
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = "http://localhost:3000/payment/vnpay-return";
        }
        if (ipAddress == null || ipAddress.isBlank()) ipAddress = "127.0.0.1";
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
        }
    }
}
