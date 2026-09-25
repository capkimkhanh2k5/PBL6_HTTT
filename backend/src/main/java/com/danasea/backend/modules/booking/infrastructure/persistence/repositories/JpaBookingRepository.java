package com.danasea.backend.modules.booking.infrastructure.persistence.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingJpaEntity;

@Repository
public interface JpaBookingRepository extends JpaRepository<BookingJpaEntity, UUID> {

    Optional<BookingJpaEntity> findByIdAndCustomerId(UUID id, UUID customerId);

    @Query("SELECT DISTINCT b FROM BookingJpaEntity b LEFT JOIN FETCH b.items WHERE b.id = :id")
    Optional<BookingJpaEntity> findByIdWithItems(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT b FROM BookingJpaEntity b LEFT JOIN FETCH b.items WHERE b.id = :id")
    Optional<BookingJpaEntity> findByIdWithItemsForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT b FROM BookingJpaEntity b WHERE b.customerId = :customerId AND (:status IS NULL OR b.status = :status)")
    Page<BookingJpaEntity> findByCustomerIdAndStatus(
            @Param("customerId") UUID customerId,
            @Param("status") BookingStatus status,
            Pageable pageable
    );

    @Query("SELECT b FROM BookingJpaEntity b WHERE b.status = com.danasea.backend.modules.booking.domain.models.BookingStatus.HOLD AND b.holdExpiresAt < :threshold")
    List<BookingJpaEntity> findExpiredHolds(@Param("threshold") OffsetDateTime threshold);
}
