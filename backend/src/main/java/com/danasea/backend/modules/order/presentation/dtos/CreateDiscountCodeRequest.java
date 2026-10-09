package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.danasea.backend.modules.order.domain.models.DiscountScope;
import com.danasea.backend.modules.order.domain.models.DiscountSponsorType;
import com.danasea.backend.modules.order.domain.models.DiscountType;

public record CreateDiscountCodeRequest(
        @NotBlank(message = "{validation.discount_code_required}")
        @Size(max = 50, message = "{validation.discount_code_size}")
        String code,

        @NotNull(message = "{validation.discount_scope_required}")
        DiscountScope scope,

        DiscountSponsorType sponsorType,

        UUID vendorId,

        UUID serviceId,

        @NotNull(message = "{validation.discount_type_required}")
        DiscountType discountType,

        @NotNull(message = "{validation.discount_value_required}")
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
