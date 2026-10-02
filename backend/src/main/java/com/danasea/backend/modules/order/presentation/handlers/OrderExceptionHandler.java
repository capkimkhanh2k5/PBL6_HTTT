package com.danasea.backend.modules.order.presentation.handlers;

import com.danasea.backend.modules.order.domain.exceptions.BookingNotEligibleForOrderException;
import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.modules.order.domain.exceptions.InvalidWebhookException;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.exceptions.PaymentVerificationException;
import com.danasea.backend.modules.order.domain.exceptions.UnauthorizedOrderAccessException;
import com.danasea.backend.shared.presentation.ErrorResponse;
import com.danasea.backend.shared.presentation.LocalizedExceptionHandlerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderExceptionHandler extends LocalizedExceptionHandlerSupport {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("ORDER_NOT_FOUND"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error("ACCESS_DENIED"));
    }

    @ExceptionHandler(UnauthorizedOrderAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedOrderAccess(UnauthorizedOrderAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error("UNAUTHORIZED_ORDER_ACCESS"));
    }

    @ExceptionHandler(BookingNotEligibleForOrderException.class)
    public ResponseEntity<ErrorResponse> handleBookingNotEligible(BookingNotEligibleForOrderException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error("BOOKING_NOT_ELIGIBLE_FOR_ORDER"));
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderState(InvalidOrderStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error("INVALID_ORDER_STATE"));
    }

    @ExceptionHandler(PaymentVerificationException.class)
    public ResponseEntity<ErrorResponse> handlePaymentVerification(PaymentVerificationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("PAYMENT_SIGNATURE_INVALID"));
    }

    @ExceptionHandler(InvalidWebhookException.class)
    public ResponseEntity<ErrorResponse> handleInvalidWebhook(InvalidWebhookException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("INVALID_WEBHOOK"));
    }
}
