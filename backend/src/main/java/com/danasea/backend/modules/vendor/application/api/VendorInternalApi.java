package com.danasea.backend.modules.vendor.application.api;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;

public interface VendorInternalApi {
    Optional<Vendor> findByUserId(UUID userId);

    Optional<Vendor> findById(UUID vendorId);

    boolean isVendorApproved(UUID vendorId);

    Vendor saveVendor(Vendor vendor);

    Page<Vendor> getVendors(VerificationStatus status, Pageable pageable);
}
