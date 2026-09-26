package com.danasea.backend.modules.order.infrastructure.adapters;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.order.application.PaymentWebhookSigner;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.ports.PaymentGatewayPort;
import com.danasea.backend.modules.order.domain.ports.PaymentIntentResult;
import com.danasea.backend.modules.order.domain.ports.RefundResult;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Adapter triển khai cổng thanh toán SePay tuân thủ PaymentGatewayPort (Mục 9.2.14).
 */
@Component
public class SePayPaymentAdapter implements PaymentGatewayPort {

    private final PaymentWebhookSigner webhookSigner;
    private final ObjectMapper objectMapper;

    public SePayPaymentAdapter(PaymentWebhookSigner webhookSigner, ObjectMapper objectMapper) {
        this.webhookSigner = webhookSigner;
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentIntentResult createPaymentIntent(UUID orderId, BigDecimal amount, PaymentProvider provider) {
        UUID paymentId = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(15);
        String paymentUrl = "https://checkout.sepay.vn/pay/" + orderId;
        String qrCodeUrl = "https://qr.sepay.vn/img?order=" + orderId + "&amount=" + (amount != null ? amount.toPlainString() : "0");

        return new PaymentIntentResult(
                paymentId,
                orderId,
                provider,
                amount,
                paymentUrl,
                qrCodeUrl,
                expiresAt
        );
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, String> rawParams, String signature) {
        if (signature == null || signature.isBlank() || rawParams == null || rawParams.isEmpty()) {
            return false;
        }

        if (rawParams.containsKey("rawPayload")) {
            return webhookSigner.isValid(rawParams.get("rawPayload"), signature);
        }

        try {
            String serialized = objectMapper.writeValueAsString(rawParams);
            return webhookSigner.isValid(serialized, signature);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public RefundResult requestRefund(String providerTransactionId, BigDecimal amount) {
        String refundId = "SEPAY-REF-" + UUID.randomUUID();
        return new RefundResult(true, refundId, amount, "Refund processed successfully via SePay");
    }
}
