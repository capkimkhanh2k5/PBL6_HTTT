package com.danasea.backend.modules.service.domain.exceptions;

import java.util.UUID;

public class UnauthorizedServiceAccessException extends ServiceDomainException {

    public UnauthorizedServiceAccessException(String message) {
        super(message);
    }

    public UnauthorizedServiceAccessException(UUID serviceId, UUID vendorId) {
        super("Vendor " + vendorId + " is not authorized to access or modify service " + serviceId);
    }
}
