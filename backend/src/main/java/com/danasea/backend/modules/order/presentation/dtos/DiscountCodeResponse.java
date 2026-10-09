package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.DiscountCode;
import com.danasea.backend.modules.order.domain.models.DiscountScope;
import com.danasea.backend.modules.order.domain.models.DiscountSponsorType;
import com.danasea.backend.modules.order.domain.models.DiscountType;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.DiscountCodeJpaEntity;

public record DiscountCodeResponse(
        UUID id,
        String code,
        DiscountScope scope,
        DiscountSponsorType sponsorType,
        UUID vendorId,
        UUID serviceId,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Integer maxUses,
        Integer usedCount,
        Integer maxUsesPerUser,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DiscountCodeResponse fromEntity(DiscountCodeJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new DiscountCodeResponse(
                entity.getId(),
                entity.getCode(),
                entity.getScope(),
                entity.getSponsorType(),
                entity.getVendorId(),
                entity.getServiceId(),
                entity.getDiscountType(),
                entity.getDiscountValue(),
                entity.getMinOrderAmount(),
                entity.getMaxDiscountAmount(),
                entity.getMaxUses(),
                entity.getUsedCount(),
                entity.getMaxUsesPerUser(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static DiscountCodeResponse fromDomain(DiscountCode domain) {
        if (domain == null) {
            return null;
        }
        return new DiscountCodeResponse(
                domain.getId(),
                domain.getCode(),
                domain.getScope(),
                domain.getSponsorType(),
                domain.getVendorId(),
                domain.getServiceId(),
                domain.getDiscountType(),
                domain.getDiscountValue(),
                domain.getMinOrderAmount(),
                domain.getMaxDiscountAmount(),
                domain.getMaxUses(),
                domain.getUsedCount(),
                domain.getMaxUsesPerUser(),
                domain.getValidFrom(),
                domain.getValidTo(),
                domain.getIsActive(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
