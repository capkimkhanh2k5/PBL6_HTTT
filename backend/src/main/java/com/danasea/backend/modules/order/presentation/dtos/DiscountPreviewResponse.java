package com.danasea.backend.modules.order.presentation.dtos;

import java.math.BigDecimal;
import java.util.List;

import com.danasea.backend.modules.order.domain.models.DiscountScope;
import com.danasea.backend.modules.order.domain.models.DiscountSponsorType;

public record DiscountPreviewResponse(
        boolean valid,
        String errorCode,
        String message,
        String code,
        DiscountScope scope,
        DiscountSponsorType sponsorType,
        BigDecimal originalTotalAmount,
        BigDecimal totalDiscountAmount,
        BigDecimal finalPayableAmount,
        List<ItemDiscountPreviewResponse> itemBreakdown
) {
    public static DiscountPreviewResponse invalid(String errorCode, String message, String code) {
        return new DiscountPreviewResponse(
                false,
                errorCode,
                message,
                code,
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of()
        );
    }
}
