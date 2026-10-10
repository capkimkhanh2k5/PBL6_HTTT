package com.danasea.backend.modules.order.presentation.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DiscountPreviewRequest(
        @NotNull(message = "{validation.booking_id_required}")
        UUID bookingId,

        @NotBlank(message = "{validation.discount_code_required}")
        String code
) {}
