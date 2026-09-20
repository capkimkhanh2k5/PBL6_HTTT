package com.danasea.backend.modules.booking.domain.ports;

import java.util.Optional;
import java.util.UUID;

public interface VendorLookupPort {
    Optional<UUID> findVendorIdByUserId(UUID userId);
}
