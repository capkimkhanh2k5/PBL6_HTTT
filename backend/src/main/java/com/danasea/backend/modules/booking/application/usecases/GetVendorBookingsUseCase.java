package com.danasea.backend.modules.booking.application.usecases;

import java.util.List;
import java.util.UUID;

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

        UUID vendorId = vendorLookupPort.findVendorIdByUserId(query.userId())
                .orElseThrow(() -> new UnauthorizedBookingAccessException("Vendor profile not found for user: " + query.userId()));

        int page = Math.max(0, query.page());
        int size = query.size() > 0 ? query.size() : 10;
        String sortBy = (query.sortBy() != null && !query.sortBy().isBlank()) ? query.sortBy() : "createdAt";
        String sortDir = (query.sortDirection() != null && !query.sortDirection().isBlank()) ? query.sortDirection() : "desc";

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
