package com.danasea.backend.modules.dispute.infrastructure.persistence.mappers;

import com.danasea.backend.modules.dispute.domain.models.Dispute;
import com.danasea.backend.modules.dispute.infrastructure.persistence.entities.DisputeJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class DisputeMapper {

    public Dispute toDomain(DisputeJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Dispute.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .subOrderId(entity.getSubOrderId())
                .customerId(entity.getCustomerId())
                .reason(entity.getReason())
                .description(entity.getDescription())
                .evidenceUrls(entity.getEvidenceUrls() != null ? new ArrayList<>(entity.getEvidenceUrls()) : new ArrayList<>())
                .status(entity.getStatus())
                .refundPercentage(entity.getRefundPercentage())
                .adminNote(entity.getAdminNote())
                .resolvedAt(entity.getResolvedAt())
                .resolvedBy(entity.getResolvedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public DisputeJpaEntity toEntity(Dispute domain) {
        if (domain == null) {
            return null;
        }

        DisputeJpaEntity entity = DisputeJpaEntity.builder()
                .orderId(domain.getOrderId())
                .subOrderId(domain.getSubOrderId())
                .customerId(domain.getCustomerId())
                .reason(domain.getReason())
                .description(domain.getDescription())
                .evidenceUrls(domain.getEvidenceUrls() != null ? new ArrayList<>(domain.getEvidenceUrls()) : new ArrayList<>())
                .status(domain.getStatus())
                .refundPercentage(domain.getRefundPercentage())
                .adminNote(domain.getAdminNote())
                .resolvedAt(domain.getResolvedAt())
                .resolvedBy(domain.getResolvedBy())
                .build();

        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }
}
