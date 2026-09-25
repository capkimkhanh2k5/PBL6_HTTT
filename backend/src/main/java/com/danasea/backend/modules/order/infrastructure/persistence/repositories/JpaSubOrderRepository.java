package com.danasea.backend.modules.order.infrastructure.persistence.repositories;

import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaSubOrderRepository extends JpaRepository<SubOrderJpaEntity, UUID> {
    List<SubOrderJpaEntity> findBySlotId(UUID slotId);
    List<SubOrderJpaEntity> findBySlotIdAndStatus(UUID slotId, com.danasea.backend.modules.order.domain.models.SubOrderStatus status);
    List<SubOrderJpaEntity> findByMasterOrderId(UUID masterOrderId);
    List<SubOrderJpaEntity> findByVendorId(UUID vendorId);
    Page<SubOrderJpaEntity> findByVendorId(UUID vendorId, Pageable pageable);
    List<SubOrderJpaEntity> findByVendorIdAndStatusIn(UUID vendorId, java.util.Collection<com.danasea.backend.modules.order.domain.models.SubOrderStatus> statuses);
    List<SubOrderJpaEntity> findByVendorIdAndCreatedAtBetween(UUID vendorId, java.time.OffsetDateTime startDateTime, java.time.OffsetDateTime endDateTime);
    java.util.Optional<SubOrderJpaEntity> findByBookingItemId(UUID bookingItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SubOrderJpaEntity s WHERE s.bookingItemId = :bookingItemId")
    java.util.Optional<SubOrderJpaEntity> findByBookingItemIdForUpdate(
            @Param("bookingItemId") UUID bookingItemId);
}
