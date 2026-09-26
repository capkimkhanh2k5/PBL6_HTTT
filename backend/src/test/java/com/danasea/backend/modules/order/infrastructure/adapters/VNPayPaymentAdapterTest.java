package com.danasea.backend.modules.order.infrastructure.adapters;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.danasea.backend.configs.properties.VNPayProperties;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.ports.PaymentIntentResult;
import com.danasea.backend.modules.order.domain.ports.RefundResult;

import static org.assertj.core.api.Assertions.assertThat;

class VNPayPaymentAdapterTest {

    private VNPayProperties vnpayProperties;
    private VNPayPaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        vnpayProperties = new VNPayProperties(
                "FOS9BJWS",
                "QNYDSCFIKKRVJIZBIARMYNARAMNHNISQ",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/payment/vnpay-return"
        );
        adapter = new VNPayPaymentAdapter(vnpayProperties);
    }

    @Test
    @DisplayName("createPaymentIntent generates valid VNPay URL with HMAC-SHA512 checksum")
    void createPaymentIntent_ShouldReturnValidVNPayPaymentIntent() {
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500000");

        PaymentIntentResult result = adapter.createPaymentIntent(orderId, amount, PaymentProvider.VNPAY);

        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.provider()).isEqualTo(PaymentProvider.VNPAY);
        assertThat(result.amount()).isEqualTo(amount);
        assertThat(result.paymentUrl()).contains("sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        assertThat(result.paymentUrl()).contains("vnp_TmnCode=FOS9BJWS");
        assertThat(result.paymentUrl()).contains("vnp_Amount=50000000");
        assertThat(result.paymentUrl()).contains("vnp_SecureHash=");
        assertThat(result.qrCodeUrl()).contains("api.qrserver.com");
    }

    @Test
    @DisplayName("verifyWebhookSignature correctly verifies HMAC-SHA512 checksum from VNPay")
    void verifyWebhookSignature_WithValidChecksum_ShouldReturnTrue() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "50000000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_Command", "pay");
        params.put("vnp_OrderInfo", "Thanh toan");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "FOS9BJWS");
        params.put("vnp_TxnRef", "test-ref-123");

        // Calculate expected hash
        String hashData = "vnp_Amount=50000000&vnp_BankCode=NCB&vnp_Command=pay&vnp_OrderInfo=Thanh+toan&vnp_ResponseCode=00&vnp_TmnCode=FOS9BJWS&vnp_TxnRef=test-ref-123";
        String validChecksum = VNPayPaymentAdapter.hmacSHA512("QNYDSCFIKKRVJIZBIARMYNARAMNHNISQ", hashData);

        boolean valid = adapter.verifyWebhookSignature(params, validChecksum);
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("verifyWebhookSignature returns false for altered data or wrong checksum")
    void verifyWebhookSignature_WithTamperedChecksum_ShouldReturnFalse() {
        Map<String, String> params = Map.of(
                "vnp_Amount", "50000000",
                "vnp_TmnCode", "FOS9BJWS"
        );

        boolean valid = adapter.verifyWebhookSignature(params, "tampered-hash-value");
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("requestRefund returns successful RefundResult")
    void requestRefund_ShouldReturnSuccess() {
        BigDecimal refundAmount = new BigDecimal("200000");
        RefundResult result = adapter.requestRefund("VNPAY-TXN-123", refundAmount);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.amount()).isEqualTo(refundAmount);
        assertThat(result.providerRefundId()).startsWith("VNPAY-REF-");
    }
}
