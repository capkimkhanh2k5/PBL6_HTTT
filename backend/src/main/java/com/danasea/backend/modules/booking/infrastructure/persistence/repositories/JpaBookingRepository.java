package com.danasea.backend.modules.booking.infrastructure.persistence.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingJpaEntity;

@Repository
public interface JpaBookingRepository extends JpaRepository<BookingJpaEntity, UUID> {

    Optional<BookingJpaEntity> findByIdAndCustomerId(UUID id, UUID customerId);

    @Query("SELECT b FROM BookingJpaEntity b WHERE b.status = com.danasea.backend.modules.booking.domain.models.BookingStatus.HOLD AND b.holdExpiresAt < :threshold")
    List<BookingJpaEntity> findExpiredHolds(@Param("threshold") OffsetDateTime threshold);
}
