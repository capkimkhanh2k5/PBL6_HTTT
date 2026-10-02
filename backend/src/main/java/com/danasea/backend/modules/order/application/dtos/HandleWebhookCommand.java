package com.danasea.backend.modules.order.application.dtos;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;

public record HandleWebhookCommand(
        PaymentProvider provider,
        Map<String, String> rawParams,
        String rawPayload,
        String signature,
        String eventId,
        UUID paymentId,
        String providerTransactionId,
        BigDecimal amount,
        PaymentStatus status
) {}
