package com.danasea.backend.modules.order.application;

import java.math.BigDecimal;

import com.danasea.backend.modules.order.domain.exceptions.InvalidDiscountException;
import com.danasea.backend.modules.order.domain.models.DiscountScope;
import com.danasea.backend.modules.order.domain.models.DiscountSponsorType;
import com.danasea.backend.modules.order.domain.models.DiscountType;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.DiscountCodeJpaEntity;

public final class DiscountCodeRules {
    private DiscountCodeRules() { }

    public static void validate(DiscountCodeJpaEntity code) {
        if (code.getScope() == null || code.getDiscountType() == null || code.getDiscountValue() == null
                || code.getDiscountValue().signum() <= 0) {
            invalid("INVALID_DISCOUNT", "Discount scope, type, and a positive value are required.");
        }
        if (code.getDiscountType() == DiscountType.PERCENTAGE
                && code.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            invalid("DISCOUNT_PERCENTAGE_INVALID", "Percentage discount cannot exceed 100.");
        }
        if (code.getValidFrom() != null && code.getValidTo() != null
                && !code.getValidTo().isAfter(code.getValidFrom())) {
            invalid("DISCOUNT_DATE_RANGE_INVALID", "Discount end time must be after its start time.");
        }
        if (code.getScope() == DiscountScope.VENDOR && code.getVendorId() == null) {
            invalid("VENDOR_ID_REQUIRED", "Vendor-scoped discounts require a vendor ID.");
        }
        if ((code.getScope() == DiscountScope.PLATFORM && code.getVendorId() != null)
                || (code.getSponsorType() == DiscountSponsorType.VENDOR && code.getScope() != DiscountScope.VENDOR)) {
            invalid("DISCOUNT_SPONSOR_SCOPE_INVALID", "Vendor sponsorship requires the corresponding vendor scope.");
        }
        if ((code.getMinOrderAmount() != null && code.getMinOrderAmount().signum() < 0)
                || (code.getMaxDiscountAmount() != null && code.getMaxDiscountAmount().signum() <= 0)
                || (code.getMaxUses() != null && code.getMaxUses() <= 0)
                || (code.getMaxUsesPerUser() != null && code.getMaxUsesPerUser() <= 0)) {
            invalid("INVALID_DISCOUNT", "Discount caps and usage limits must be positive.");
        }
    }

    private static void invalid(String code, String message) {
        throw new InvalidDiscountException(code, message);
    }
}
