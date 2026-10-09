package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

public record UpdateDiscountCodeRequest(
        @DecimalMin(value = "0.01", message = "{validation.discount_value_positive}")
        BigDecimal discountValue,

        @DecimalMin(value = "0.00", message = "{validation.discount_min_nonnegative}")
        BigDecimal minOrderAmount,

        @DecimalMin(value = "0.01", message = "{validation.discount_cap_positive}")
        BigDecimal maxDiscountAmount,

        @Positive(message = "{validation.discount_uses_positive}")
        Integer maxUses,

        @Positive(message = "{validation.discount_user_uses_positive}")
        Integer maxUsesPerUser,

        OffsetDateTime validFrom,

        OffsetDateTime validTo,

        Boolean isActive
) {}
