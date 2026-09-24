package com.danasea.backend.modules.order.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaMasterOrderRepository extends JpaRepository<MasterOrderJpaEntity, UUID> {

    Optional<MasterOrderJpaEntity> findByBookingId(UUID bookingId);

    Optional<MasterOrderJpaEntity> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    Page<MasterOrderJpaEntity> findByCustomerId(UUID customerId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM MasterOrderJpaEntity o WHERE o.id = :id")
    Optional<MasterOrderJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
