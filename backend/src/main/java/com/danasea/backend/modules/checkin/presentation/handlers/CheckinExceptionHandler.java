package com.danasea.backend.modules.checkin.presentation.handlers;

import com.danasea.backend.modules.checkin.domain.exceptions.CheckinAlreadyUsedException;
import com.danasea.backend.modules.checkin.domain.exceptions.CheckinTokenNotFoundException;
import com.danasea.backend.modules.checkin.domain.exceptions.InvalidCheckinStateException;
import com.danasea.backend.modules.checkin.domain.exceptions.InvalidQrSignatureException;
import com.danasea.backend.modules.checkin.domain.exceptions.InvalidQrTokenException;
import com.danasea.backend.modules.checkin.domain.exceptions.InvalidSubOrderStateException;
import com.danasea.backend.modules.checkin.domain.exceptions.QrTokenExpiredException;
import com.danasea.backend.modules.checkin.domain.exceptions.UnauthorizedCheckinAccessException;
import com.danasea.backend.modules.checkin.domain.exceptions.UnauthorizedVendorCheckinException;
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

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.danasea.backend.modules.checkin")
public class CheckinExceptionHandler {

    public record CheckinAlreadyUsedErrorResponse(
            String code,
            String message,
            OffsetDateTime usedAt,
            UUID usedByVendorStaffId
    ) {}

    @ExceptionHandler(InvalidQrSignatureException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQrSignature(InvalidQrSignatureException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_QR_SIGNATURE", ex.getMessage()));
    }

    @ExceptionHandler(InvalidQrTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQrToken(InvalidQrTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_QR_TOKEN", ex.getMessage()));
    }

    @ExceptionHandler(InvalidSubOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSubOrderState(InvalidSubOrderStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_SUB_ORDER_STATE", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCheckinStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCheckinState(InvalidCheckinStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("SLOT_ALREADY_CONCLUDED", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedCheckinAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedCheckinAccess(UnauthorizedCheckinAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedVendorCheckinException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedVendorCheckin(UnauthorizedVendorCheckinException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("VENDOR_MISMATCH", ex.getMessage()));
    }

    @ExceptionHandler(CheckinAlreadyUsedException.class)
    public ResponseEntity<CheckinAlreadyUsedErrorResponse> handleCheckinAlreadyUsed(CheckinAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new CheckinAlreadyUsedErrorResponse(
                        "QR_TOKEN_ALREADY_USED",
                        ex.getMessage(),
                        ex.getUsedAt(),
                        ex.getUsedByVendorStaffId()
                ));
    }

    @ExceptionHandler(QrTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleQrTokenExpired(QrTokenExpiredException ex) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(new ErrorResponse("QR_TOKEN_EXPIRED", ex.getMessage()));
    }

    @ExceptionHandler(CheckinTokenNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCheckinTokenNotFound(CheckinTokenNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("CHECKIN_TOKEN_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ORDER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_INPUT", msg));
    }
}
