package com.danasea.backend.modules.order.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.PaymentJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByMasterOrderIdAndIdempotencyKey(UUID masterOrderId, String idempotencyKey);

    Optional<PaymentJpaEntity> findByProviderAndWebhookEventId(PaymentProvider provider, String webhookEventId);

    List<PaymentJpaEntity> findByMasterOrderIdOrderByCreatedAtDesc(UUID masterOrderId);

    boolean existsByMasterOrderIdAndStatus(UUID masterOrderId, PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.id = :id")
    Optional<PaymentJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
