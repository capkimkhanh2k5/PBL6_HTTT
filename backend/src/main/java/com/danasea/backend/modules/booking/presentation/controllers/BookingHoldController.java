package com.danasea.backend.modules.booking.presentation.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.booking.application.dtos.BookingHoldItemDto;
import com.danasea.backend.modules.booking.application.dtos.BookingHoldResult;
import com.danasea.backend.modules.booking.application.dtos.CreateBookingHoldCommand;
import com.danasea.backend.modules.booking.application.usecases.CreateBookingHoldUseCase;
import com.danasea.backend.modules.booking.presentation.dtos.BookingHoldItemResponse;
import com.danasea.backend.modules.booking.presentation.dtos.BookingHoldRequest;
import com.danasea.backend.modules.booking.presentation.dtos.BookingHoldResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingHoldController {

    private final CreateBookingHoldUseCase createBookingHoldUseCase;

    @PostMapping("/hold")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<BookingHoldResponse> holdBooking(
            @Valid @RequestBody BookingHoldRequest request) {

        UUID customerId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));

        List<BookingHoldItemDto> itemDtos = request.items().stream()
                .map(item -> new BookingHoldItemDto(item.slotId(), item.quantity()))
                .toList();

        CreateBookingHoldCommand command = new CreateBookingHoldCommand(customerId, itemDtos);
        BookingHoldResult result = createBookingHoldUseCase.execute(command);

        List<BookingHoldItemResponse> itemResponses = result.items().stream()
                .map(i -> new BookingHoldItemResponse(
                        i.id(),
                        i.serviceId(),
                        i.vendorId(),
                        i.slotId(),
                        i.quantity(),
                        i.bookingDate(),
                        i.bookingTime(),
                        i.price(),
                        i.subtotal()
                ))
                .toList();

        BookingHoldResponse response = new BookingHoldResponse(
                result.bookingId(),
                result.customerId(),
                result.status(),
                result.totalAmount(),
                result.holdExpiresAt(),
                itemResponses,
                result.createdAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
