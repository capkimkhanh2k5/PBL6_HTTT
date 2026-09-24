package com.danasea.backend.modules.dispute.presentation.handlers;

import com.danasea.backend.modules.dispute.domain.exceptions.*;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.shared.presentation.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.danasea.backend.modules.dispute")
public class DisputeExceptionHandler {

    @ExceptionHandler(DisputeAlreadyResolvedException.class)
    public ResponseEntity<ErrorResponse> handleDisputeAlreadyResolved(DisputeAlreadyResolvedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DISPUTE_ALREADY_RESOLVED", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateDisputeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateDispute(DuplicateDisputeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ACTIVE_DISPUTE_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(DisputeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDisputeNotFound(DisputeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("DISPUTE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ORDER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedDisputeAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedDisputeAccess(UnauthorizedDisputeAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(InvalidSubOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSubOrderState(InvalidSubOrderStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_SUB_ORDER_STATE", ex.getMessage()));
    }

    @ExceptionHandler(DisputePeriodExpiredException.class)
    public ResponseEntity<ErrorResponse> handleDisputePeriodExpired(DisputePeriodExpiredException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("DISPUTE_PERIOD_EXPIRED", ex.getMessage()));
    }

    @ExceptionHandler(InvalidDisputeResolutionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDisputeResolution(InvalidDisputeResolutionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_DISPUTE_RESOLUTION", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_ARGUMENT", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_INPUT", message));
    }
}
