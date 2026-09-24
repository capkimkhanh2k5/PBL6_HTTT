package com.danasea.backend.modules.booking.application.usecases;

import java.util.List;
import java.util.UUID;
import java.util.Set;

import com.danasea.backend.modules.booking.application.dtos.GetVendorBookingsQuery;
import com.danasea.backend.modules.booking.application.dtos.VendorBookingItemResult;
import com.danasea.backend.modules.booking.domain.exceptions.UnauthorizedBookingAccessException;
import com.danasea.backend.modules.booking.domain.models.BookingItem;
import com.danasea.backend.modules.booking.domain.models.PagedResult;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.domain.ports.VendorLookupPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetVendorBookingsUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final VendorLookupPort vendorLookupPort;

    public PagedResult<VendorBookingItemResult> execute(GetVendorBookingsQuery query) {
        if (query == null || query.userId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
        int page = query.page();
        int size = query.size();
        String sortBy = (query.sortBy() != null && !query.sortBy().isBlank()) ? query.sortBy() : "createdAt";
        String sortDir = (query.sortDirection() != null && !query.sortDirection().isBlank()) ? query.sortDirection() : "desc";
        if (!Set.of("createdAt", "updatedAt", "bookingDate", "bookingTime", "price").contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported sort field: " + sortBy);
        }
        if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("sortDir must be either asc or desc");
        }

        UUID vendorId = vendorLookupPort.findVendorIdByUserId(query.userId())
                .orElseThrow(() -> new UnauthorizedBookingAccessException("Vendor profile not found for user: " + query.userId()));

        PagedResult<BookingItem> pagedItems = bookingRepository.findVendorBookingItems(
                vendorId,
                query.status(),
                page,
                size,
                sortBy,
                sortDir
        );

        List<VendorBookingItemResult> results = pagedItems.content().stream()
                .map(item -> new VendorBookingItemResult(
                        item.getId(),
                        item.getBookingId(),
                        item.getServiceId(),
                        item.getVendorId(),
                        item.getSlotId(),
                        item.getQuantity(),
                        item.getBookingDate(),
                        item.getBookingTime(),
                        item.getPrice(),
                        item.calculateSubtotal(),
                        item.getBookingStatus(),
                        item.getCreatedAt()
                ))
                .toList();

        return new PagedResult<>(
                results,
                pagedItems.page(),
                pagedItems.size(),
                pagedItems.totalElements(),
                pagedItems.totalPages()
        );
    }
}
