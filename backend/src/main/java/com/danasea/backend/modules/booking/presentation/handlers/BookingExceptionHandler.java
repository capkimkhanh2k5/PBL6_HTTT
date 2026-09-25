package com.danasea.backend.modules.booking.presentation.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.danasea.backend.modules.booking.domain.exceptions.BookingDomainException;
import com.danasea.backend.modules.booking.domain.exceptions.BookingHoldExpiredException;
import com.danasea.backend.modules.booking.domain.exceptions.BookingNotFoundException;
import com.danasea.backend.modules.booking.domain.exceptions.InsufficientInventoryException;
import com.danasea.backend.modules.booking.domain.exceptions.InvalidBookingStateException;
import com.danasea.backend.modules.booking.domain.exceptions.SlotNotAvailableException;
import com.danasea.backend.modules.booking.domain.exceptions.UnauthorizedBookingAccessException;
import com.danasea.backend.shared.presentation.ErrorResponse;
import com.danasea.backend.shared.presentation.LocalizedExceptionHandlerSupport;

@RestControllerAdvice
public class BookingExceptionHandler extends LocalizedExceptionHandlerSupport {

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientInventory(InsufficientInventoryException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error("INSUFFICIENT_INVENTORY"));
    }

    @ExceptionHandler(SlotNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleSlotNotAvailable(SlotNotAvailableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("SLOT_NOT_AVAILABLE"));
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookingNotFound(BookingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("BOOKING_NOT_FOUND"));
    }

    @ExceptionHandler(BookingHoldExpiredException.class)
    public ResponseEntity<ErrorResponse> handleBookingHoldExpired(BookingHoldExpiredException ex) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(error("BOOKING_HOLD_EXPIRED"));
    }

    @ExceptionHandler(InvalidBookingStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBookingState(InvalidBookingStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("INVALID_BOOKING_STATE"));
    }

    @ExceptionHandler(UnauthorizedBookingAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(UnauthorizedBookingAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error("UNAUTHORIZED_BOOKING_ACCESS"));
    }

    @ExceptionHandler(BookingDomainException.class)
    public ResponseEntity<ErrorResponse> handleBookingDomainException(BookingDomainException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("BOOKING_ERROR"));
    }
}
