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
}
