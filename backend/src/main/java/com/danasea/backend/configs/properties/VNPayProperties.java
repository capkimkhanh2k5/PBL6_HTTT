package com.danasea.backend.configs.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình tham số cổng thanh toán VNPay Sandbox.
 * <p>
 * Luồng cấu hình (Traceability Map):
 * <ul>
 *   <li>Biến môi trường: {@code VNPAY_TMN_CODE}, {@code VNPAY_HASH_SECRET}, {@code VNPAY_PAY_URL}, {@code VNPAY_RETURN_URL} (trong {@code .env})</li>
 *   <li>File cấu hình: {@code backend/src/main/resources/application.yml} (mục {@code app.payment.vnpay})</li>
 *   <li>Sử dụng tại: {@code VNPayPaymentAdapter.java} để khởi tạo giao dịch VNPay-QR và tính mã băm HMAC-SHA512 checksum</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.payment.vnpay")
public record VNPayProperties(
        String tmnCode,
        String hashSecret,
        String payUrl,
        String returnUrl
) {
    public VNPayProperties {
        if (payUrl == null || payUrl.isBlank()) {
            payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        }
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = "http://localhost:3000/payment/vnpay-return";
        }
    }
}
