package com.danasea.backend.modules.vendor.application.ports;

import java.util.UUID;

public interface VendorActiveServicesPort {
    long countActiveServices(UUID vendorId);
}
