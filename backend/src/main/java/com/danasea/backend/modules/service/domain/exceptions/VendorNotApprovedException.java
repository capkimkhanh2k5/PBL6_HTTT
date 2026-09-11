package com.danasea.backend.modules.service.domain.exceptions;

import java.util.UUID;

public class VendorNotApprovedException extends ServiceDomainException {

    public VendorNotApprovedException(UUID vendorId) {
        super("Vendor is not approved: " + vendorId);
    }

    public VendorNotApprovedException(String message) {
        super(message);
    }
}
