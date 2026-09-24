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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SettlementExceptionHandler Unit Tests")
class SettlementExceptionHandlerTest {

    private final SettlementExceptionHandler handler = new SettlementExceptionHandler();

    @BeforeEach
    void setLocale() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("SettlementAlreadyFinalizedException maps to 409 CONFLICT")
    void handleSettlementAlreadyFinalized() {
        SettlementAlreadyFinalizedException ex = new SettlementAlreadyFinalizedException("Period already finalized");
        ResponseEntity<ErrorResponse> response = handler.handleSettlementAlreadyFinalized(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("SETTLEMENT_ALREADY_FINALIZED");
        assertThat(response.getBody().message()).isEqualTo("The settlement has already been finalized.");
    }

    @Test
    @DisplayName("SettlementAlreadyPaidException maps to 409 CONFLICT")
    void handleSettlementAlreadyPaid() {
        SettlementAlreadyPaidException ex = new SettlementAlreadyPaidException("Period already paid");
        ResponseEntity<ErrorResponse> response = handler.handleSettlementAlreadyPaid(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("SETTLEMENT_ALREADY_PAID");
    }

    @Test
    @DisplayName("SettlementNotFoundException maps to 404 NOT FOUND")
    void handleSettlementNotFound() {
        SettlementNotFoundException ex = new SettlementNotFoundException("Settlement not found");
        ResponseEntity<ErrorResponse> response = handler.handleSettlementNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("SETTLEMENT_NOT_FOUND");
    }

    @Test
    @DisplayName("VendorNotFoundException maps to 404 NOT FOUND")
    void handleVendorNotFound() {
        VendorNotFoundException ex = new VendorNotFoundException("Vendor not found");
        ResponseEntity<ErrorResponse> response = handler.handleVendorNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VENDOR_NOT_FOUND");
    }

    @Test
    @DisplayName("UnauthorizedSettlementAccessException maps to 403 FORBIDDEN")
    void handleUnauthorizedSettlementAccess() {
        UnauthorizedSettlementAccessException ex = new UnauthorizedSettlementAccessException("Forbidden cross-vendor access");
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorizedSettlementAccess(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("FORBIDDEN_SETTLEMENT_ACCESS");
    }

    @Test
    @DisplayName("InvalidSettlementPeriodException maps to 400 BAD REQUEST")
    void handleInvalidSettlementPeriod() {
        InvalidSettlementPeriodException ex = new InvalidSettlementPeriodException("Start date after end date");
        ResponseEntity<ErrorResponse> response = handler.handleInvalidSettlementPeriod(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_SETTLEMENT_PERIOD");
    }

    @Test
    @DisplayName("InvalidCommissionRateException maps to 400 BAD REQUEST")
    void handleInvalidCommissionRate() {
        InvalidCommissionRateException ex = new InvalidCommissionRateException("Rate out of bounds");
        ResponseEntity<ErrorResponse> response = handler.handleInvalidCommissionRate(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_COMMISSION_RATE");
    }

    @Test
    @DisplayName("InvalidOrderAmountException maps to 400 BAD REQUEST")
    void handleInvalidOrderAmount() {
        InvalidOrderAmountException ex = new InvalidOrderAmountException("Amount cannot be negative");
        ResponseEntity<ErrorResponse> response = handler.handleInvalidOrderAmount(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_ORDER_AMOUNT");
    }
}
