package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;

public record PaymentResponse(
        UUID id,
        UUID masterOrderId,
        PaymentProvider provider,
        String providerTransactionId,
        BigDecimal amount,
        PaymentStatus status,
        String idempotencyKey,
        String webhookEventId,
        String paymentUrl,
        String qrCodeUrl,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String providerOrderId,
        String lastError,
        Integer reconciliationAttempts,
        OffsetDateTime reconciliationNextAttemptAt,
        OffsetDateTime lastReconciledAt
) {
    public PaymentResponse(
            UUID id,
            UUID masterOrderId,
            PaymentProvider provider,
            String providerTransactionId,
            BigDecimal amount,
            PaymentStatus status,
            String idempotencyKey,
            String webhookEventId,
            String paymentUrl,
            String qrCodeUrl,
            OffsetDateTime expiresAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String providerOrderId) {
        this(id, masterOrderId, provider, providerTransactionId, amount, status, idempotencyKey, webhookEventId, paymentUrl, qrCodeUrl,
                expiresAt, createdAt, updatedAt, providerOrderId, null, null, null, null);
    }

    public PaymentResponse(
            UUID id,
            UUID masterOrderId,
            PaymentProvider provider,
            String providerTransactionId,
            BigDecimal amount,
            PaymentStatus status,
            String idempotencyKey,
            String webhookEventId,
            String paymentUrl,
            String qrCodeUrl,
            OffsetDateTime expiresAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this(id, masterOrderId, provider, providerTransactionId, amount, status, idempotencyKey,
                webhookEventId, paymentUrl, qrCodeUrl, expiresAt, createdAt, updatedAt, null);
    }
}
