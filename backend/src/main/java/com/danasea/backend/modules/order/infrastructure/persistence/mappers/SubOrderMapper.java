package com.danasea.backend.modules.order.infrastructure.persistence.mappers;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;

@Component
public class SubOrderMapper {

    public SubOrder toDomain(SubOrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        SubOrder domain = new SubOrder();
        domain.setId(entity.getId());
        domain.setBookingItemId(entity.getBookingItemId());
        domain.setMasterOrderId(entity.getMasterOrderId());
        domain.setVendorId(entity.getVendorId());
        domain.setServiceId(entity.getServiceId());
        domain.setSlotId(entity.getSlotId());
        domain.setQuantity(entity.getQuantity());
        domain.setUnitPrice(entity.getUnitPrice());
        domain.setSubtotalAmount(entity.getSubtotalAmount());
        domain.setCommissionRate(entity.getCommissionRate());
        domain.setCommissionAmount(entity.getCommissionAmount());
        domain.setVendorPayoutAmount(entity.getVendorPayoutAmount());
        domain.setStatus(entity.getStatus());
        domain.setWaiverAccepted(entity.getWaiverAccepted());
        domain.setWaiverAcceptedAt(entity.getWaiverAcceptedAt());
        domain.setQrSecret(entity.getQrSecret());
        domain.setCheckedInAt(entity.getCheckedInAt());
        domain.setVendorNotifiedAt(entity.getVendorNotifiedAt());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    public SubOrderJpaEntity toEntity(SubOrder domain) {
        if (domain == null) {
            return null;
        }
        SubOrderJpaEntity entity = new SubOrderJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        entity.setBookingItemId(domain.getBookingItemId());
        entity.setMasterOrderId(domain.getMasterOrderId());
        entity.setVendorId(domain.getVendorId());
        entity.setServiceId(domain.getServiceId());
        entity.setSlotId(domain.getSlotId());
        entity.setQuantity(domain.getQuantity());
        entity.setUnitPrice(domain.getUnitPrice());
        entity.setSubtotalAmount(domain.getSubtotalAmount());
        entity.setCommissionRate(domain.getCommissionRate());
        entity.setCommissionAmount(domain.getCommissionAmount());
        entity.setVendorPayoutAmount(domain.getVendorPayoutAmount());
        entity.setStatus(domain.getStatus());
        entity.setWaiverAccepted(domain.getWaiverAccepted());
        entity.setWaiverAcceptedAt(domain.getWaiverAcceptedAt());
        entity.setQrSecret(domain.getQrSecret());
        entity.setCheckedInAt(domain.getCheckedInAt());
        entity.setVendorNotifiedAt(domain.getVendorNotifiedAt());
        if (domain.getCreatedAt() != null) {
            entity.setCreatedAt(domain.getCreatedAt());
        }
        if (domain.getUpdatedAt() != null) {
            entity.setUpdatedAt(domain.getUpdatedAt());
        }
        return entity;
    }
}
