package com.danasea.backend.modules.service.presentation.dtos;

import java.math.BigDecimal;

import com.danasea.backend.modules.service.domain.models.OptionStatus;
import jakarta.validation.constraints.DecimalMin;

public record UpdateServiceOptionRequest(
        String name,

        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        Integer maxPaxPerPackage,

        String benefits,

        OptionStatus status
) {}
