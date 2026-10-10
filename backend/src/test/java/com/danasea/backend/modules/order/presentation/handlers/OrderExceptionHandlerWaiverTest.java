package com.danasea.backend.modules.order.presentation.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.danasea.backend.modules.order.domain.exceptions.WaiverAcceptanceRequiredException;
import com.danasea.backend.modules.order.domain.exceptions.WaiverVersionMismatchException;
import com.danasea.backend.modules.order.domain.models.MissingWaiverItem;
import com.danasea.backend.modules.order.presentation.dtos.WaiverAcceptanceRequiredResponse;
import com.danasea.backend.shared.presentation.ErrorResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@DisplayName("OrderExceptionHandler Waiver Exception Tests")
class OrderExceptionHandlerWaiverTest {

    private final OrderExceptionHandler handler = new OrderExceptionHandler();

    @Test
    @DisplayName(
            "WaiverAcceptanceRequiredException -> 409 Conflict với code WAIVER_ACCEPTANCE_REQUIRED"
                    + " và danh sách chi tiết")
    void handleWaiverAcceptanceRequired() {
        UUID orderId = UUID.randomUUID();
        UUID subOrderId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        MissingWaiverItem item =
                new MissingWaiverItem(
                        subOrderId,
                        serviceId,
                        "Tour Lặn Biển",
                        1,
                        true,
                        "Cam kết an toàn khi lặn",
                        "VI",
                        false);

        WaiverAcceptanceRequiredException ex =
                new WaiverAcceptanceRequiredException(orderId, List.of(item));

        ResponseEntity<WaiverAcceptanceRequiredResponse> response =
                handler.handleWaiverAcceptanceRequired(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        WaiverAcceptanceRequiredResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("WAIVER_ACCEPTANCE_REQUIRED");
        assertThat(body.orderId()).isEqualTo(orderId);
        assertThat(body.missingSubOrders()).hasSize(1);

        var firstItem = body.missingSubOrders().get(0);
        assertThat(firstItem.subOrderId()).isEqualTo(subOrderId);
        assertThat(firstItem.serviceId()).isEqualTo(serviceId);
        assertThat(firstItem.serviceName()).isEqualTo("Tour Lặn Biển");
        assertThat(firstItem.waiverVersion()).isEqualTo(1);
        assertThat(firstItem.required()).isTrue();
        assertThat(firstItem.waiverContent()).isEqualTo("Cam kết an toàn khi lặn");
        assertThat(firstItem.contentLanguage()).isEqualTo("VI");
        assertThat(firstItem.fallbackUsed()).isFalse();
    }

    @Test
    @DisplayName("WaiverVersionMismatchException -> 409 Conflict với code WAIVER_VERSION_MISMATCH")
    void handleWaiverVersionMismatch() {
        WaiverVersionMismatchException ex =
                new WaiverVersionMismatchException("Waiver version mismatch");

        ResponseEntity<ErrorResponse> response = handler.handleWaiverVersionMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("WAIVER_VERSION_MISMATCH");
    }
}
