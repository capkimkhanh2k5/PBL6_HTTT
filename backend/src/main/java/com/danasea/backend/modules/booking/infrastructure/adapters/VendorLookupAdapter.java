package com.danasea.backend.modules.booking.infrastructure.adapters;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.booking.domain.ports.VendorLookupPort;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VendorLookupAdapter implements VendorLookupPort {

    private final VendorInternalApi vendorInternalApi;

    @Override
    public Optional<UUID> findVendorIdByUserId(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return vendorInternalApi.findByUserId(userId).map(Vendor::getId);
    }
}
