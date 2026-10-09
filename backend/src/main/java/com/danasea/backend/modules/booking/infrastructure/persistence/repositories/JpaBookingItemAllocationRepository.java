package com.danasea.backend.modules.booking.infrastructure.persistence.repositories;

import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingItemAllocationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaBookingItemAllocationRepository extends JpaRepository<BookingItemAllocationJpaEntity, UUID> {

    List<BookingItemAllocationJpaEntity> findByBookingItemId(UUID bookingItemId);

    List<BookingItemAllocationJpaEntity> findBySlotId(UUID slotId);

    void deleteByBookingItemId(UUID bookingItemId);
}
