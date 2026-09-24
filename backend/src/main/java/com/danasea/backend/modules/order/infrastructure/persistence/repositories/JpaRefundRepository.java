package com.danasea.backend.modules.order.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaRefundRepository extends JpaRepository<RefundJpaEntity, UUID> {
    java.util.List<RefundJpaEntity> findBySubOrderId(UUID subOrderId);
    java.util.List<RefundJpaEntity> findBySubOrderIdIn(java.util.Collection<UUID> subOrderIds);
}
