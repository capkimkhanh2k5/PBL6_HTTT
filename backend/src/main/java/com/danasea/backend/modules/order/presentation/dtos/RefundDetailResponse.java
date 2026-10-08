package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;

public record RefundDetailResponse(
        UUID id,
        UUID subOrderId,
        BigDecimal amount,
        BigDecimal percentage,
        RefundReason reason,
        RefundStatus status,
        PaymentProvider provider,
        String providerRefundId,
        String providerTransactionId,
        Integer retryCount,
        String lastError,
        OffsetDateTime processedAt,
        OffsetDateTime createdAt
) {
    public static RefundDetailResponse fromEntity(RefundJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RefundDetailResponse(
                entity.getId(),
                entity.getSubOrderId(),
                entity.getAmount(),
                entity.getRefundPercentage(),
                entity.getReason(),
                entity.getStatus(),
                entity.getProvider(),
                entity.getProviderRefundId(),
                entity.getProviderTransactionId(),
                entity.getRetryCount(),
                entity.getLastError(),
                entity.getProcessedAt(),
                entity.getCreatedAt()
        );
    }
}
