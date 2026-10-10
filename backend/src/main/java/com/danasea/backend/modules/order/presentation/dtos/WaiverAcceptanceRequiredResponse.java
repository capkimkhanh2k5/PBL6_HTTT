package com.danasea.backend.modules.order.presentation.dtos;

import java.util.List;
import java.util.UUID;

public record WaiverAcceptanceRequiredResponse(
        String code,
        String message,
        UUID orderId,
        List<MissingWaiverSubOrderResponse> missingSubOrders) {
    public static final String ERROR_CODE = "WAIVER_ACCEPTANCE_REQUIRED";
}
