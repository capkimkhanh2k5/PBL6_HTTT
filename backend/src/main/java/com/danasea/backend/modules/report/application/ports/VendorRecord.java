package com.danasea.backend.modules.report.application.ports;

import com.danasea.backend.modules.vendor.domain.models.BadgeTier;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;

import java.math.BigDecimal;
import java.util.UUID;

/** Projection record chứa thông tin Vendor phục vụ báo cáo hiệu suất đối tác. */
public record VendorRecord(
        UUID id,
        UUID userId,
        String businessName,
        VerificationStatus verificationStatus,
        BigDecimal ratingAvg,
        Integer ratingCount,
        BadgeTier badgeTier) {}
