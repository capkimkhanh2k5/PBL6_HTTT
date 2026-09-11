package com.danasea.backend.modules.vendor.application.api;

import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.vendor.domain.models.Vendor;

public interface VendorInternalApi {
    Optional<Vendor> findByUserId(UUID userId);

    Optional<Vendor> findById(UUID vendorId);

    boolean isVendorApproved(UUID vendorId);
}
