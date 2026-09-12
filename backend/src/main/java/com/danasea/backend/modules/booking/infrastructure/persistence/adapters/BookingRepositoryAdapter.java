package com.danasea.backend.modules.booking.infrastructure.persistence.adapters;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingJpaEntity;
import com.danasea.backend.modules.booking.infrastructure.persistence.mappers.BookingMapper;
import com.danasea.backend.modules.booking.infrastructure.persistence.repositories.JpaBookingRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookingRepositoryAdapter implements BookingRepositoryPort {

    private final JpaBookingRepository jpaBookingRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public Booking save(Booking booking) {
        BookingJpaEntity entity = bookingMapper.toEntity(booking);
        BookingJpaEntity saved = jpaBookingRepository.save(entity);
        return bookingMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Booking> findById(UUID id) {
        return jpaBookingRepository.findById(id).map(bookingMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Booking> findByIdAndCustomerId(UUID id, UUID customerId) {
        return jpaBookingRepository.findByIdAndCustomerId(id, customerId).map(bookingMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findExpiredHolds(OffsetDateTime threshold) {
        return jpaBookingRepository.findExpiredHolds(threshold).stream()
                .map(bookingMapper::toDomain)
                .toList();
    }
}
