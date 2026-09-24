package com.danasea.backend.modules.order.infrastructure.persistence.repositories;

import com.danasea.backend.modules.order.infrastructure.persistence.entities.RefundJpaEntity;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaRefundRepository extends JpaRepository<RefundJpaEntity, UUID> {
    java.util.List<RefundJpaEntity> findBySubOrderId(UUID subOrderId);
    java.util.List<RefundJpaEntity> findBySubOrderIdIn(java.util.Collection<UUID> subOrderIds);
    Optional<RefundJpaEntity> findBySubOrderIdAndIdempotencyKey(UUID subOrderId, String idempotencyKey);
    Optional<RefundJpaEntity> findByProviderAndWebhookEventId(PaymentProvider provider, String webhookEventId);
    java.util.List<RefundJpaEntity> findBySubOrderIdInAndStatus(
            java.util.Collection<UUID> subOrderIds, RefundStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefundJpaEntity r WHERE r.id = :id")
    Optional<RefundJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
