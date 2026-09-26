package com.danasea.backend.modules.order.domain.ports;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;

/**
 * Domain Port giao tiếp với các cổng thanh toán bên ngoài (SePay, VNPay, MoMo).
 * Tuân thủ quy chuẩn Mục 9.2.14.
 */
public interface PaymentGatewayPort {

    PaymentIntentResult createPaymentIntent(UUID orderId, BigDecimal amount, PaymentProvider provider);

    boolean verifyWebhookSignature(Map<String, String> rawParams, String signature);

    RefundResult requestRefund(String providerTransactionId, BigDecimal amount);
}
