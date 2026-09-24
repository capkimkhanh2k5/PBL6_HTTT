package com.danasea.backend.modules.order.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaSubOrderRepository extends JpaRepository<SubOrderJpaEntity, UUID> {
    List<SubOrderJpaEntity> findBySlotId(UUID slotId);
    List<SubOrderJpaEntity> findBySlotIdAndStatus(UUID slotId, com.danasea.backend.modules.order.domain.models.SubOrderStatus status);
    List<SubOrderJpaEntity> findByMasterOrderId(UUID masterOrderId);
    List<SubOrderJpaEntity> findByVendorId(UUID vendorId);
    List<SubOrderJpaEntity> findByVendorIdAndStatusIn(UUID vendorId, java.util.Collection<com.danasea.backend.modules.order.domain.models.SubOrderStatus> statuses);
    List<SubOrderJpaEntity> findByVendorIdAndCreatedAtBetween(UUID vendorId, java.time.OffsetDateTime startDateTime, java.time.OffsetDateTime endDateTime);
}
