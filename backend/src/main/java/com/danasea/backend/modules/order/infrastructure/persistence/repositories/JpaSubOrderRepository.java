package com.danasea.backend.modules.order.infrastructure.persistence.repositories;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaSubOrderRepository extends JpaRepository<SubOrderJpaEntity, UUID> {
    List<SubOrderJpaEntity> findBySlotId(UUID slotId);
    List<SubOrderJpaEntity> findBySlotIdAndStatus(UUID slotId, SubOrderStatus status);
    List<SubOrderJpaEntity> findByMasterOrderId(UUID masterOrderId);
    List<SubOrderJpaEntity> findByVendorId(UUID vendorId);
    Page<SubOrderJpaEntity> findByVendorId(UUID vendorId, Pageable pageable);
    List<SubOrderJpaEntity> findByVendorIdAndStatusIn(UUID vendorId, Collection<SubOrderStatus> statuses);
    List<SubOrderJpaEntity> findByVendorIdAndCreatedAtBetween(UUID vendorId, OffsetDateTime startDateTime, OffsetDateTime endDateTime);
    Optional<SubOrderJpaEntity> findByBookingItemId(UUID bookingItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SubOrderJpaEntity s WHERE s.bookingItemId = :bookingItemId")
    Optional<SubOrderJpaEntity> findByBookingItemIdForUpdate(
            @Param("bookingItemId") UUID bookingItemId);
}

