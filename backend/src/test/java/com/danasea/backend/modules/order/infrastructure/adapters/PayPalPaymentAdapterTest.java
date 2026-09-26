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
import org.springframework.web.client.RestClient;

import com.danasea.backend.configs.properties.PayPalProperties;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.ports.PaymentIntentResult;
import com.danasea.backend.modules.order.domain.ports.RefundResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PayPalPaymentAdapterTest {

    @Mock
    private SePayPaymentAdapter sePayPaymentAdapter;

    private PayPalProperties payPalProperties;
    private ObjectMapper objectMapper;
    private PayPalPaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        payPalProperties = new PayPalProperties(
                "sandbox",
                "BAAcyzDuInaGyvEGc-QvrX15iuHLHwtMy8kaHy3HT7edLoksPIsXxaXEvm920NAwNzFzirWf7MQ-Q3e_7g",
                "ELGW-cx_uqbcNh_kTfTsKjR-YzSYSX47Lth7lj36YoqZAPES2Ol4fVO73lqX6nIO_YWTsI3XXhCmmKlJ",
                "MOCK-WEBHOOK-ID",
                "https://api-m.sandbox.paypal.com",
                "http://localhost:3000/payment/success",
                "http://localhost:3000/payment/cancel"
        );
        objectMapper = new ObjectMapper();
        adapter = new PayPalPaymentAdapter(
                RestClient.builder(),
                payPalProperties,
                objectMapper,
                sePayPaymentAdapter
        );
    }

    @Test
    @DisplayName("createPaymentIntent converts VND to USD and produces valid PayPal payment and QR URL")
    void createPaymentIntent_ShouldReturnValidPayPalPaymentIntent() {
        UUID orderId = UUID.randomUUID();
        BigDecimal vndAmount = new BigDecimal("500000");

        PaymentIntentResult result = adapter.createPaymentIntent(orderId, vndAmount, PaymentProvider.PAYPAL);

        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.provider()).isEqualTo(PaymentProvider.PAYPAL);
        assertThat(result.amount()).isEqualTo(vndAmount);
        assertThat(result.paymentUrl()).contains("sandbox.paypal.com");
        assertThat(result.qrCodeUrl()).contains("api.qrserver.com");
        assertThat(result.expiresAt()).isNotNull();
    }

    @Test
    @DisplayName("verifyWebhookSignature returns true for valid PayPal signature")
    void verifyWebhookSignature_ValidSignature_ShouldReturnTrue() {
        Map<String, String> rawParams = Map.of(
                "event_type", "PAYMENT.CAPTURE.COMPLETED",
                "rawPayload", "{\"id\":\"WH-123\"}"
        );

        boolean valid = adapter.verifyWebhookSignature(rawParams, "valid-paypal-transmission-sig");
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("verifyWebhookSignature returns false for invalid signature or empty inputs")
    void verifyWebhookSignature_InvalidSignature_ShouldReturnFalse() {
        assertThat(adapter.verifyWebhookSignature(null, "sig")).isFalse();
        assertThat(adapter.verifyWebhookSignature(Map.of(), "sig")).isFalse();
        assertThat(adapter.verifyWebhookSignature(Map.of("k", "v"), "")).isFalse();
        assertThat(adapter.verifyWebhookSignature(Map.of("k", "v"), "invalid-signature")).isFalse();
    }

    @Test
    @DisplayName("requestRefund returns successful RefundResult")
    void requestRefund_ShouldReturnSuccess() {
        BigDecimal refundAmount = new BigDecimal("250000");
        RefundResult result = adapter.requestRefund("CAPTURE-12345", refundAmount);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.amount()).isEqualTo(refundAmount);
        assertThat(result.providerRefundId()).startsWith("PAYPAL-REF-");
    }
}
