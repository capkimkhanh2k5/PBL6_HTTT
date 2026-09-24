package com.danasea.backend.modules.settlement.presentation.handlers;

import com.danasea.backend.modules.settlement.domain.exceptions.InvalidCommissionRateException;
import com.danasea.backend.modules.settlement.domain.exceptions.InvalidOrderAmountException;
import com.danasea.backend.modules.settlement.domain.exceptions.InvalidSettlementPeriodException;
import com.danasea.backend.modules.settlement.domain.exceptions.SettlementAlreadyFinalizedException;
import com.danasea.backend.modules.settlement.domain.exceptions.SettlementAlreadyPaidException;
import com.danasea.backend.modules.settlement.domain.exceptions.SettlementNotFoundException;
import com.danasea.backend.modules.settlement.domain.exceptions.UnauthorizedSettlementAccessException;
import com.danasea.backend.modules.vendor.domain.exceptions.VendorNotFoundException;
import com.danasea.backend.shared.presentation.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.danasea.backend.modules.settlement")
public class SettlementExceptionHandler {

    @ExceptionHandler(SettlementNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSettlementNotFound(SettlementNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("SETTLEMENT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(SettlementAlreadyFinalizedException.class)
    public ResponseEntity<ErrorResponse> handleSettlementAlreadyFinalized(SettlementAlreadyFinalizedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("SETTLEMENT_ALREADY_FINALIZED", ex.getMessage()));
    }

    @ExceptionHandler(SettlementAlreadyPaidException.class)
    public ResponseEntity<ErrorResponse> handleSettlementAlreadyPaid(SettlementAlreadyPaidException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("SETTLEMENT_ALREADY_PAID", ex.getMessage()));
    }

    @ExceptionHandler(VendorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVendorNotFound(VendorNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("VENDOR_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidSettlementPeriodException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSettlementPeriod(InvalidSettlementPeriodException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_SETTLEMENT_PERIOD", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCommissionRateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCommissionRate(InvalidCommissionRateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_COMMISSION_RATE", ex.getMessage()));
    }

    @ExceptionHandler(InvalidOrderAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderAmount(InvalidOrderAmountException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_ORDER_AMOUNT", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedSettlementAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedSettlementAccess(UnauthorizedSettlementAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN_SETTLEMENT_ACCESS", ex.getMessage()));
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
