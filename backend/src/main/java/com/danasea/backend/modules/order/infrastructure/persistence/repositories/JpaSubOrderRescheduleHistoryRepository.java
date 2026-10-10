package com.danasea.backend.modules.order.infrastructure.persistence.repositories;

import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderRescheduleHistoryJpaEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaSubOrderRescheduleHistoryRepository
        extends JpaRepository<SubOrderRescheduleHistoryJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM SubOrderRescheduleHistoryJpaEntity e WHERE e.id = :id")
    Optional<SubOrderRescheduleHistoryJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    List<SubOrderRescheduleHistoryJpaEntity>
            findTop50ByNotificationSentAtIsNullOrderByCreatedAtAsc();

    Optional<SubOrderRescheduleHistoryJpaEntity> findBySubOrderIdAndIdempotencyKey(
            UUID subOrderId, String idempotencyKey);

    List<SubOrderRescheduleHistoryJpaEntity> findBySubOrderIdOrderByCreatedAtDesc(UUID subOrderId);
}
