package com.danasea.backend.modules.booking.presentation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorRejectBookingRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
