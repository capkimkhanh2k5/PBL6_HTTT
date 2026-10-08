package com.danasea.backend.modules.service.presentation.dtos;

import java.math.BigDecimal;

import com.danasea.backend.modules.service.domain.models.OptionType;
import com.danasea.backend.modules.service.domain.models.PricingUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateServiceOptionRequest(
        @NotBlank(message = "Option name must not be blank")
        String name,

        @NotNull(message = "Option type (SHARED/PRIVATE) must not be null")
        OptionType optionType,

        @NotNull(message = "Pricing unit (PER_PERSON/PER_PACKAGE) must not be null")
        PricingUnit pricingUnit,

        @NotNull(message = "Price must not be null")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        Integer maxPaxPerPackage,

        String benefits
) {}
