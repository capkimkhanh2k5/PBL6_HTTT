package com.danasea.backend.modules.service.presentation.dtos;

import java.math.BigDecimal;

import com.danasea.backend.modules.service.domain.models.OptionType;
import com.danasea.backend.modules.service.domain.models.PricingUnit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateServiceOptionRequest(
        @NotBlank(message = "{validation.service.option.name.required}")
        String name,

        @NotNull(message = "{validation.service.option.type.required}")
        OptionType optionType,

        @NotNull(message = "{validation.service.option.pricing.required}")
        PricingUnit pricingUnit,

        @NotNull(message = "{validation.service.option.price.required}")
        @DecimalMin(value = "0.0", inclusive = false, message = "{validation.service.option.price.positive}")
        BigDecimal price,

        Integer maxPaxPerPackage,

        String benefits
) {}
