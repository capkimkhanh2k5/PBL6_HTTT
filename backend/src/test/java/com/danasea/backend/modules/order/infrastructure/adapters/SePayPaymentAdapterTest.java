package com.danasea.backend.modules.order.infrastructure.adapters;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.order.application.PaymentWebhookSigner;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.ports.PaymentIntentResult;
import com.danasea.backend.modules.order.domain.ports.RefundResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SePayPaymentAdapterTest {

    @Mock
    private PaymentWebhookSigner webhookSigner;

    private ObjectMapper objectMapper;
    private SePayPaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new SePayPaymentAdapter(webhookSigner, objectMapper);
    }

    @Test
    @DisplayName("createPaymentIntent generates valid payment URL and QR code URL")
    void createPaymentIntent_ShouldReturnValidResult() {
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("250000");

        PaymentIntentResult result = adapter.createPaymentIntent(orderId, amount, PaymentProvider.SEPAY);

        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.provider()).isEqualTo(PaymentProvider.SEPAY);
        assertThat(result.amount()).isEqualTo(amount);
        assertThat(result.paymentUrl()).contains(orderId.toString());
        assertThat(result.qrCodeUrl()).contains(orderId.toString());
        assertThat(result.expiresAt()).isNotNull();
    }

    @Test
    @DisplayName("verifyWebhookSignature validates signature with rawPayload if present")
    void verifyWebhookSignature_WithRawPayload_ShouldCallWebhookSigner() {
        String rawPayload = "{\"gateway\":\"sepay\",\"amount\":250000}";
        String signature = "valid-sig";
        Map<String, String> rawParams = Map.of("rawPayload", rawPayload);

        when(webhookSigner.isValid(rawPayload, signature)).thenReturn(true);

        boolean valid = adapter.verifyWebhookSignature(rawParams, signature);

        assertThat(valid).isTrue();
        verify(webhookSigner).isValid(rawPayload, signature);
    }

    @Test
    @DisplayName("verifyWebhookSignature validates signature by serializing params when rawPayload is missing")
    void verifyWebhookSignature_WithoutRawPayload_ShouldSerializeAndVerify() {
        Map<String, String> rawParams = Map.of("orderId", "123", "amount", "50000");
        String signature = "sig";

        when(webhookSigner.isValid(anyString(), eq(signature))).thenReturn(true);

        boolean valid = adapter.verifyWebhookSignature(rawParams, signature);

        assertThat(valid).isTrue();
        verify(webhookSigner).isValid(anyString(), eq(signature));
    }

    @Test
    @DisplayName("verifyWebhookSignature returns false for empty params or blank signature")
    void verifyWebhookSignature_InvalidInputs_ShouldReturnFalse() {
        assertThat(adapter.verifyWebhookSignature(null, "sig")).isFalse();
        assertThat(adapter.verifyWebhookSignature(Map.of(), "sig")).isFalse();
        assertThat(adapter.verifyWebhookSignature(Map.of("k", "v"), "")).isFalse();
        assertThat(adapter.verifyWebhookSignature(Map.of("k", "v"), null)).isFalse();
    }

    @Test
    @DisplayName("requestRefund returns successful RefundResult")
    void requestRefund_ShouldReturnSuccess() {
        BigDecimal amount = new BigDecimal("100000");
        RefundResult result = adapter.requestRefund("TXN-12345", amount);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.amount()).isEqualTo(amount);
        assertThat(result.providerRefundId()).startsWith("SEPAY-REF-");
    }
}
