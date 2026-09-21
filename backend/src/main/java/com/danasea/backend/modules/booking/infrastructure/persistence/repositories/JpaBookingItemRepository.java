package com.danasea.backend.modules.booking.infrastructure.persistence.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingItemJpaEntity;

@Repository
public interface JpaBookingItemRepository extends JpaRepository<BookingItemJpaEntity, UUID> {

    List<BookingItemJpaEntity> findByBookingId(UUID bookingId);

    List<BookingItemJpaEntity> findBySlotId(UUID slotId);

    @EntityGraph(attributePaths = {"booking"})
    @Query("SELECT bi FROM BookingItemJpaEntity bi WHERE bi.vendorId = :vendorId AND (:status IS NULL OR bi.booking.status = :status)")
    Page<BookingItemJpaEntity> findByVendorIdAndStatus(
            @Param("vendorId") UUID vendorId,
            @Param("status") BookingStatus status,
            Pageable pageable
    );
}
