package com.danasea.backend.modules.order.infrastructure.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.danasea.backend.configs.properties.PayPalProperties;
import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.modules.order.domain.exceptions.PaymentGatewayException;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.ports.GatewayRefundRequest;
import com.danasea.backend.modules.order.domain.ports.GatewayRefundStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PayPalPaymentAdapterTest {

    private static final String BASE = "https://api-m.sandbox.paypal.com";

    @Mock
    private VNPayPaymentAdapter vnPayPaymentAdapter;

    private MockRestServiceServer server;
    private PayPalPaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new PayPalPaymentAdapter(builder,
                new PayPalProperties("sandbox", "test-id", "test-secret", "WH-CONFIGURED", BASE, null, null),
                new ObjectMapper(), vnPayPaymentAdapter);
    }

    private void token() {
        server.expect(requestTo(BASE + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"test-token\"}", MediaType.APPLICATION_JSON));
    }

    @Test
    void createsRealApprovalLinkWithStableProviderIdempotencyKey() {
        UUID paymentId = UUID.randomUUID();
        token();
        server.expect(requestTo(BASE + "/v2/checkout/orders"))
                .andExpect(header("PayPal-Request-Id", paymentId.toString()))
                .andExpect(content().json("{\"purchase_units\":[{\"custom_id\":\"" + paymentId
                        + "\",\"reference_id\":\"" + paymentId + "\",\"amount\":{\"currency_code\":\"USD\",\"value\":\"19.31\"}}]}"))
                .andRespond(withSuccess("{\"id\":\"REAL-SANDBOX-ID\",\"links\":[{\"rel\":\"approve\",\"href\":\"https://www.sandbox.paypal.com/checkoutnow?token=REAL-SANDBOX-ID\"}]}", MediaType.APPLICATION_JSON));
        var result = adapter.createPaymentIntent(paymentId, UUID.randomUUID(), new BigDecimal("500000"), PaymentProvider.PAYPAL);
        assertThat(result.paymentId()).isEqualTo(paymentId);
        assertThat(result.paymentUrl()).endsWith("REAL-SANDBOX-ID");
        server.verify();
    }

    @Test
    void authenticationFailureDoesNotProduceSimulatedTokenOrUrl() {
        server.expect(requestTo(BASE + "/v1/oauth2/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> adapter.createPaymentIntent(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("500000"), PaymentProvider.PAYPAL)).isInstanceOf(PaymentGatewayException.class);
        server.verify();
    }

    @Test
    void orderFailureDoesNotProduceSimulatedUrl() {
        token();
        server.expect(requestTo(BASE + "/v2/checkout/orders")).andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));
        assertThatThrownBy(() -> adapter.createPaymentIntent(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("500000"), PaymentProvider.PAYPAL)).isInstanceOf(PaymentGatewayException.class);
        server.verify();
    }

    @Test
    void missingApprovalLinkIsRejected() {
        token();
        server.expect(requestTo(BASE + "/v2/checkout/orders"))
                .andRespond(withSuccess("{\"id\":\"NO-LINK\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adapter.createPaymentIntent(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("500000"), PaymentProvider.PAYPAL)).isInstanceOf(PaymentGatewayException.class);
        server.verify();
    }

    @Test
    void unimplementedProvidersAreRejectedInsteadOfRoutedToPayPal() {
        for (PaymentProvider provider : new PaymentProvider[] {PaymentProvider.MOMO, PaymentProvider.SEPAY}) {
            assertThatThrownBy(() -> adapter.createPaymentIntent(UUID.randomUUID(), UUID.randomUUID(),
                    BigDecimal.TEN, provider)).isInstanceOf(InvalidOrderStateException.class);
        }
        server.verify();
    }

    @Test
    void vnpayReceivesThePersistedPaymentId() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        adapter.createPaymentIntent(paymentId, orderId, BigDecimal.TEN, PaymentProvider.VNPAY);
        verify(vnPayPaymentAdapter).createPaymentIntent(paymentId, orderId, BigDecimal.TEN, PaymentProvider.VNPAY);
        server.verify();
    }

    @Test
    void missingProviderVerificationMetadataFailsClosed() {
        assertThat(adapter.verifyWebhookSignature(null, "sig")).isFalse();
        assertThat(adapter.verifyWebhookSignature(Map.of(), "sig")).isFalse();
        assertThat(adapter.verifyWebhookSignature(Map.of("rawPayload", "{}"), "valid-paypal-sig")).isFalse();
        server.verify();
    }

    @Test
    void verifiesTheWebhookEventAsAnObjectUsingAllTransmissionHeaders() {
        token();
        server.expect(requestTo(BASE + "/v1/notifications/verify-webhook-signature"))
                .andExpect(content().json("{\"webhook_event\":{\"id\":\"WH-EVENT\"},\"webhook_id\":\"WH-CONFIGURED\",\"transmission_sig\":\"signed\"}"))
                .andRespond(withSuccess("{\"verification_status\":\"SUCCESS\"}", MediaType.APPLICATION_JSON));
        assertThat(adapter.verifyWebhookSignature(Map.of("paypal-transmission-id", "transmission-id",
                "paypal-transmission-time", "2026-10-06T00:00:00Z", "paypal-cert-url", "https://api.paypal.com/cert.pem",
                "paypal-auth-algo", "SHA256withRSA", "rawPayload", "{\"id\":\"WH-EVENT\"}"), "signed")).isTrue();
        server.verify();
    }

    @Test
    void refundUsesTheProviderCaptureId() {
        token();
        server.expect(requestTo(BASE + "/v2/payments/captures/CAPTURE-123/refund"))
                .andRespond(withSuccess("{\"id\":\"PROVIDER-REFUND-ID\",\"status\":\"COMPLETED\",\"amount\":{\"value\":\"9.66\",\"currency_code\":\"USD\"}}", MediaType.APPLICATION_JSON));
        var result = adapter.requestRefund(new GatewayRefundRequest(PaymentProvider.PAYPAL, "CAPTURE-123", "ORDER-1", null, new BigDecimal("9.66"), "USD", false, "persisted-refund-operation"));
        assertThat(result.success()).isTrue();
        assertThat(result.providerRefundId()).isEqualTo("PROVIDER-REFUND-ID");
        server.verify();
    }

    @Test
    void captureOrderSuccessfullyReturnsCompletedResult() {
        token();
        server.expect(requestTo(BASE + "/v2/checkout/orders/PAYPAL-ORDER-1/capture"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("PayPal-Request-Id", "IDEM-KEY-1"))
                .andRespond(withSuccess("{\"id\":\"PAYPAL-ORDER-1\",\"status\":\"COMPLETED\",\"purchase_units\":[{\"payments\":{\"captures\":[{\"id\":\"CAP-999\",\"status\":\"COMPLETED\",\"amount\":{\"currency_code\":\"USD\",\"value\":\"20.00\"}}]}}]}", MediaType.APPLICATION_JSON));

        var result = adapter.captureOrder(PaymentProvider.PAYPAL, "PAYPAL-ORDER-1", "IDEM-KEY-1");
        assertThat(result.success()).isTrue();
        assertThat(result.captureId()).isEqualTo("CAP-999");
        assertThat(result.status()).isEqualTo("COMPLETED");
        server.verify();
    }

    @Test
    void captureOrderAlreadyCapturedReconcilesViaOrderLookup() {
        token();
        server.expect(requestTo(BASE + "/v2/checkout/orders/PAYPAL-ORDER-2/capture"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"details\":[{\"issue\":\"ORDER_ALREADY_CAPTURED\"}]}"));
        token();

        server.expect(requestTo(BASE + "/v2/checkout/orders/PAYPAL-ORDER-2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"PAYPAL-ORDER-2\",\"status\":\"COMPLETED\",\"purchase_units\":[{\"payments\":{\"captures\":[{\"id\":\"CAP-RECONCILED\",\"status\":\"COMPLETED\",\"amount\":{\"currency_code\":\"USD\",\"value\":\"20.00\"}}]}}]}", MediaType.APPLICATION_JSON));

        var result = adapter.captureOrder(PaymentProvider.PAYPAL, "PAYPAL-ORDER-2", "IDEM-KEY-2");
        assertThat(result.success()).isTrue();
        assertThat(result.captureId()).isEqualTo("CAP-RECONCILED");
        server.verify();
    }

    @Test
    void pendingRefundDoesNotReportCompletedFunds() {
        token();
        server.expect(requestTo(BASE + "/v2/payments/captures/CAPTURE-123/refund"))
                .andExpect(header("PayPal-Request-Id", "persisted-operation"))
                .andExpect(content().json("{\"amount\":{\"value\":\"0.04\",\"currency_code\":\"USD\"},\"invoice_id\":\"persisted-operation\"}"))
                .andRespond(withSuccess("{\"id\":\"PENDING-REFUND\",\"status\":\"PENDING\",\"amount\":{\"value\":\"0.04\",\"currency_code\":\"USD\"}}", MediaType.APPLICATION_JSON));
        var result = adapter.requestRefund(new GatewayRefundRequest(PaymentProvider.PAYPAL, "CAPTURE-123", "ORDER-1", null,
                new BigDecimal("0.04"), "USD", false, "persisted-operation"));
        assertThat(result.success()).isFalse();
        assertThat(result.status()).isEqualTo(GatewayRefundStatus.PENDING);
        server.verify();
    }

    @Test
    void queryWithoutRefundIdIsUnknownAndDoesNotSendAnyFinancialCommand() {
        var result = adapter.queryRefund(new GatewayRefundRequest(PaymentProvider.PAYPAL, "CAPTURE-123", "ORDER-1", null,
                new BigDecimal("9.66"), "USD", false, "persisted-operation"), null);
        assertThat(result.status()).isEqualTo(GatewayRefundStatus.UNKNOWN);
        assertThat(result.success()).isFalse();
        server.verify();
    }

    @Test
    void arbitraryUnprocessableCaptureErrorDoesNotPretendAlreadyCaptured() {
        token();
        server.expect(requestTo(BASE + "/v2/checkout/orders/ORDER-1/capture"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"details\":[{\"issue\":\"ORDER_NOT_APPROVED\"}]}"));
        assertThatThrownBy(() -> adapter.captureOrder(PaymentProvider.PAYPAL, "ORDER-1", "persisted-operation"))
                .isInstanceOf(PaymentGatewayException.class);
        server.verify();
    }
}
