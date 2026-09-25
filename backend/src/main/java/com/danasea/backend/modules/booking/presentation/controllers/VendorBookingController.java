package com.danasea.backend.modules.booking.presentation.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.danasea.backend.modules.booking.application.dtos.GetVendorBookingsQuery;
import com.danasea.backend.modules.booking.application.dtos.VendorBookingItemResult;
import com.danasea.backend.modules.booking.application.usecases.GetVendorBookingsUseCase;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.models.PagedResult;
import com.danasea.backend.modules.booking.presentation.dtos.PageResponse;
import com.danasea.backend.modules.booking.presentation.dtos.VendorBookingItemResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import com.danasea.backend.modules.order.application.OrderPaymentService;
import com.danasea.backend.modules.order.presentation.dtos.SubOrderResponse;
import com.danasea.backend.modules.booking.presentation.dtos.VendorRejectBookingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/vendor/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorBookingController {

    private final GetVendorBookingsUseCase getVendorBookingsUseCase;
    private final OrderPaymentService orderPaymentService;

    @PatchMapping("/{id}/reject")
    public ResponseEntity<SubOrderResponse> rejectBooking(
            @PathVariable("id") UUID bookingItemId,
            @Valid @RequestBody VendorRejectBookingRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));
        String effectiveKey = idempotencyKey == null || idempotencyKey.isBlank()
                ? "vendor-reject-" + bookingItemId
                : idempotencyKey;
        return ResponseEntity.ok(orderPaymentService.rejectVendorBooking(
                userId, bookingItemId, request.reason(), effectiveKey));
    }

    @GetMapping
    public ResponseEntity<PageResponse<VendorBookingItemResponse>> getVendorBookings(
            @RequestParam(name = "status", required = false) BookingStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));

        GetVendorBookingsQuery query = new GetVendorBookingsQuery(
                currentUserId,
                status,
                page,
                size,
                sortBy,
                sortDir
        );

        PagedResult<VendorBookingItemResult> result = getVendorBookingsUseCase.execute(query);

        List<VendorBookingItemResponse> content = result.content().stream()
                .map(item -> new VendorBookingItemResponse(
                        item.itemId(),
                        item.bookingId(),
                        item.serviceId(),
                        item.vendorId(),
                        item.slotId(),
                        item.quantity(),
                        item.bookingDate(),
                        item.bookingTime(),
                        item.price(),
                        item.subtotal(),
                        item.bookingStatus(),
                        item.createdAt()
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
