package com.danasea.backend.modules.order.presentation.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PayPalCaptureRequest(
        @NotNull(message = "{validation.payment.id.required}")
        UUID paymentId,

        @NotBlank(message = "{validation.payment.paypal_order_id.required}")
        String paypalOrderId
) {}
