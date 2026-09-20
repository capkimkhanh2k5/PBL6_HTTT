package com.danasea.backend.modules.booking.presentation.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.booking.application.dtos.BookingDetailResult;
import com.danasea.backend.modules.booking.application.dtos.BookingSummaryResult;
import com.danasea.backend.modules.booking.application.dtos.GetBookingDetailQuery;
import com.danasea.backend.modules.booking.application.dtos.GetCustomerBookingsQuery;
import com.danasea.backend.modules.booking.application.usecases.GetBookingDetailUseCase;
import com.danasea.backend.modules.booking.application.usecases.GetCustomerBookingsUseCase;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.models.PagedResult;
import com.danasea.backend.modules.booking.presentation.dtos.BookingDetailResponse;
import com.danasea.backend.modules.booking.presentation.dtos.BookingHoldItemResponse;
import com.danasea.backend.modules.booking.presentation.dtos.BookingSummaryResponse;
import com.danasea.backend.modules.booking.presentation.dtos.PageResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final GetBookingDetailUseCase getBookingDetailUseCase;
    private final GetCustomerBookingsUseCase getCustomerBookingsUseCase;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingDetailResponse> getBookingDetail(@PathVariable("id") UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        GetBookingDetailQuery query = new GetBookingDetailQuery(id, currentUserId, isAdmin);
        BookingDetailResult result = getBookingDetailUseCase.execute(query);

        List<BookingHoldItemResponse> items = result.items().stream()
                .map(item -> new BookingHoldItemResponse(
                        item.id(),
                        item.serviceId(),
                        item.vendorId(),
                        item.slotId(),
                        item.quantity(),
                        item.bookingDate(),
                        item.bookingTime(),
                        item.price(),
                        item.subtotal()
                ))
                .toList();

        BookingDetailResponse response = new BookingDetailResponse(
                result.bookingId(),
                result.customerId(),
                result.status(),
                result.totalAmount(),
                result.holdExpiresAt(),
                items,
                result.createdAt(),
                result.updatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PageResponse<BookingSummaryResponse>> getMyBookings(
            @RequestParam(name = "status", required = false) BookingStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));

        GetCustomerBookingsQuery query = new GetCustomerBookingsQuery(
                currentUserId,
                status,
                page,
                size,
                sortBy,
                sortDir
        );

        PagedResult<BookingSummaryResult> result = getCustomerBookingsUseCase.execute(query);

        List<BookingSummaryResponse> content = result.content().stream()
                .map(s -> new BookingSummaryResponse(
                        s.bookingId(),
                        s.customerId(),
                        s.status(),
                        s.totalAmount(),
                        s.totalItems(),
                        s.holdExpiresAt(),
                        s.createdAt()
                ))
                .toList();

        return ResponseEntity.ok(PageResponse.of(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        ));
    }
}
