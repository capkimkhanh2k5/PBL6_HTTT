package com.danasea.backend.modules.booking.infrastructure.persistence.adapters;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.BookingItem;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.models.PagedResult;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingItemJpaEntity;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingJpaEntity;
import com.danasea.backend.modules.booking.infrastructure.persistence.mappers.BookingMapper;
import com.danasea.backend.modules.booking.infrastructure.persistence.repositories.JpaBookingItemRepository;
import com.danasea.backend.modules.booking.infrastructure.persistence.repositories.JpaBookingRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookingRepositoryAdapter implements BookingRepositoryPort {

    private final JpaBookingRepository jpaBookingRepository;
    private final JpaBookingItemRepository jpaBookingItemRepository;
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
    public Optional<Booking> findByIdWithItems(UUID id) {
        return jpaBookingRepository.findByIdWithItems(id).map(bookingMapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<Booking> findByIdWithItemsForUpdate(UUID id) {
        return jpaBookingRepository.findByIdWithItemsForUpdate(id).map(bookingMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<Booking> findCustomerBookings(
            UUID customerId,
            BookingStatus status,
            int page,
            int size,
            String sortBy,
            String sortDirection) {

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        Page<BookingJpaEntity> entityPage = jpaBookingRepository.findByCustomerIdAndStatus(customerId, status, pageable);

        List<Booking> domainList = entityPage.getContent().stream()
                .map(bookingMapper::toDomain)
                .toList();

        return PagedResult.of(domainList, page, size, entityPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<BookingItem> findVendorBookingItems(
            UUID vendorId,
            BookingStatus status,
            int page,
            int size,
            String sortBy,
            String sortDirection) {

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        Page<BookingItemJpaEntity> entityPage = jpaBookingItemRepository.findByVendorIdAndStatus(vendorId, status, pageable);

        List<BookingItem> domainList = entityPage.getContent().stream()
                .map(bookingMapper::toDomainItem)
                .toList();

        return PagedResult.of(domainList, page, size, entityPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findExpiredHolds(OffsetDateTime threshold) {
        return jpaBookingRepository.findExpiredHolds(threshold).stream()
                .map(bookingMapper::toDomain)
                .toList();
    }
}
