package com.danasea.backend.modules.order.infrastructure.persistence.mappers;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.PaymentOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;

@Component
public class MasterOrderMapper {

    public MasterOrder toDomain(MasterOrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        MasterOrder domain = new MasterOrder();
        domain.setId(entity.getId());
        domain.setBookingId(entity.getBookingId());
        domain.setCustomerId(entity.getCustomerId());
        domain.setStatus(entity.getStatus());
        domain.setPaymentStatus(entity.getPaymentStatus() != null ? entity.getPaymentStatus() : PaymentOrderStatus.UNPAID);
        domain.setTotalAmount(entity.getTotalAmount());
        domain.setDiscountAmount(entity.getDiscountAmount());
        domain.setDiscountCodeId(entity.getDiscountCodeId());
        domain.setPaymentDeadline(entity.getPaymentDeadline());
        domain.setIdempotencyKey(entity.getIdempotencyKey());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    public MasterOrderJpaEntity toEntity(MasterOrder domain) {
        if (domain == null) {
            return null;
        }
        MasterOrderJpaEntity entity = new MasterOrderJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        entity.setBookingId(domain.getBookingId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setStatus(domain.getStatus());
        entity.setPaymentStatus(domain.getPaymentStatus() != null ? domain.getPaymentStatus() : PaymentOrderStatus.UNPAID);
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setDiscountAmount(domain.getDiscountAmount());
        entity.setDiscountCodeId(domain.getDiscountCodeId());
        entity.setPaymentDeadline(domain.getPaymentDeadline());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        if (domain.getCreatedAt() != null) {
            entity.setCreatedAt(domain.getCreatedAt());
        }
        if (domain.getUpdatedAt() != null) {
            entity.setUpdatedAt(domain.getUpdatedAt());
        }
        return entity;
    }
}
