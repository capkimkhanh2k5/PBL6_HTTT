package com.danasea.backend.modules.booking.infrastructure.persistence.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingItemJpaEntity;

@Repository
public interface JpaBookingItemRepository extends JpaRepository<BookingItemJpaEntity, UUID> {

    List<BookingItemJpaEntity> findByBookingId(UUID bookingId);

    List<BookingItemJpaEntity> findBySlotId(UUID slotId);
}
