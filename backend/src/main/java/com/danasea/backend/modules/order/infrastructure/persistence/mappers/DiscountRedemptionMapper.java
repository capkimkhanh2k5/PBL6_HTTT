package com.danasea.backend.modules.order.infrastructure.persistence.mappers;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.order.domain.models.DiscountRedemption;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.DiscountRedemptionJpaEntity;

@Component
public class DiscountRedemptionMapper {

    public DiscountRedemption toDomain(DiscountRedemptionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        DiscountRedemption domain = new DiscountRedemption();
        domain.setId(entity.getId());
        domain.setDiscountCodeId(entity.getDiscountCodeId());
        domain.setMasterOrderId(entity.getMasterOrderId());
        domain.setCustomerId(entity.getCustomerId());
        domain.setAmountDeducted(entity.getAmountDeducted());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    public DiscountRedemptionJpaEntity toEntity(DiscountRedemption domain) {
        if (domain == null) {
            return null;
        }
        DiscountRedemptionJpaEntity entity = new DiscountRedemptionJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        entity.setDiscountCodeId(domain.getDiscountCodeId());
        entity.setMasterOrderId(domain.getMasterOrderId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setAmountDeducted(domain.getAmountDeducted());
        if (domain.getCreatedAt() != null) {
            entity.setCreatedAt(domain.getCreatedAt());
        }
        if (domain.getUpdatedAt() != null) {
            entity.setUpdatedAt(domain.getUpdatedAt());
        }
        return entity;
    }
}
