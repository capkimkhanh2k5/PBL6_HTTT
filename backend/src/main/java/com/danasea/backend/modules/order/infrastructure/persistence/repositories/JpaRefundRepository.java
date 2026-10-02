package com.danasea.backend.modules.order.infrastructure.persistence.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaRefundRepository extends JpaRepository<RefundJpaEntity, UUID> {
    List<RefundJpaEntity> findBySubOrderId(UUID subOrderId);
    List<RefundJpaEntity> findBySubOrderIdIn(Collection<UUID> subOrderIds);
    Optional<RefundJpaEntity> findBySubOrderIdAndIdempotencyKey(UUID subOrderId, String idempotencyKey);
    Optional<RefundJpaEntity> findByProviderAndWebhookEventId(PaymentProvider provider, String webhookEventId);
    List<RefundJpaEntity> findBySubOrderIdInAndStatus(
            Collection<UUID> subOrderIds, RefundStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefundJpaEntity r WHERE r.id = :id")
    Optional<RefundJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}

