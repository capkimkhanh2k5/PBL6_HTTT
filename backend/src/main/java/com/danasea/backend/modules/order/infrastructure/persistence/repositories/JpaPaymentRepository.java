package com.danasea.backend.modules.order.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.PaymentJpaEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface JpaPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    List<PaymentJpaEntity> findTop50ByProviderAndStatusAndCaptureRequestedAtIsNotNullOrderByCaptureRequestedAtAsc(
            PaymentProvider provider, PaymentStatus status);

    Optional<PaymentJpaEntity> findByMasterOrderIdAndIdempotencyKey(UUID masterOrderId, String idempotencyKey);

    Optional<PaymentJpaEntity> findByProviderAndWebhookEventId(PaymentProvider provider, String webhookEventId);

    Optional<PaymentJpaEntity> findByProviderAndProviderTransactionId(PaymentProvider provider, String providerTransactionId);

    Optional<PaymentJpaEntity> findByProviderAndProviderOrderId(PaymentProvider provider, String providerOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.provider = :provider AND p.providerOrderId = :providerOrderId")
    Optional<PaymentJpaEntity> findByProviderAndProviderOrderIdForUpdate(
            @Param("provider") PaymentProvider provider,
            @Param("providerOrderId") String providerOrderId);

    Optional<PaymentJpaEntity> findByProviderOrderId(String providerOrderId);

    List<PaymentJpaEntity> findByMasterOrderIdOrderByCreatedAtDesc(UUID masterOrderId);

    boolean existsByMasterOrderIdAndStatus(UUID masterOrderId, PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.id = :id")
    Optional<PaymentJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT p FROM PaymentJpaEntity p WHERE "
            + "(:status IS NULL OR p.status = :status) AND "
            + "(:provider IS NULL OR p.provider = :provider) AND "
            + "(:masterOrderId IS NULL OR p.masterOrderId = :masterOrderId)")
    Page<PaymentJpaEntity> findByFilters(
            @Param("status") PaymentStatus status,
            @Param("provider") PaymentProvider provider,
            @Param("masterOrderId") UUID masterOrderId,
            Pageable pageable);
}
