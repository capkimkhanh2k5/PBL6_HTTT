package com.danasea.backend.modules.booking.application.usecases;

import java.util.List;
import java.util.Set;

import com.danasea.backend.modules.booking.application.dtos.BookingSummaryResult;
import com.danasea.backend.modules.booking.application.dtos.GetCustomerBookingsQuery;
import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.PagedResult;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetCustomerBookingsUseCase {

    private final BookingRepositoryPort bookingRepository;

    public PagedResult<BookingSummaryResult> execute(GetCustomerBookingsQuery query) {
        if (query == null || query.customerId() == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
        int page = query.page();
        int size = query.size();
        String sortBy = (query.sortBy() != null && !query.sortBy().isBlank()) ? query.sortBy() : "createdAt";
        String sortDir = (query.sortDirection() != null && !query.sortDirection().isBlank()) ? query.sortDirection() : "desc";
        if (!Set.of("createdAt", "updatedAt", "holdExpiresAt", "totalAmount", "status").contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported sort field: " + sortBy);
        }
        if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("sortDir must be either asc or desc");
        }

        PagedResult<Booking> pagedBookings = bookingRepository.findCustomerBookings(
                query.customerId(),
                query.status(),
                page,
                size,
                sortBy,
                sortDir
        );

        List<BookingSummaryResult> summaryList = pagedBookings.content().stream()
                .map(b -> new BookingSummaryResult(
                        b.getId(),
                        b.getCustomerId(),
                        b.getStatus(),
                        b.getTotalAmount(),
                        b.getItems() != null ? b.getItems().size() : 0,
                        b.getHoldExpiresAt(),
                        b.getCreatedAt()
                ))
                .toList();

        return new PagedResult<>(
                summaryList,
                pagedBookings.page(),
                pagedBookings.size(),
                pagedBookings.totalElements(),
                pagedBookings.totalPages()
        );
    }
}
