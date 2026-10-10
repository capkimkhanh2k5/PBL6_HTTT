package com.danasea.backend.modules.vendor.presentation.dtos;

import com.danasea.backend.modules.vendor.domain.models.BadgeTier;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PublicVendorResponse(
        UUID id,
        String businessName,
        String address,
        BadgeTier badgeTier,
        BigDecimal ratingAvg,
        Integer ratingCount,
        long activeServicesCount
) {}
