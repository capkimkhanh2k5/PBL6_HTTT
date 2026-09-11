package com.danasea.backend.modules.service.domain.exceptions;

import java.util.UUID;

public class ServiceNotFoundException extends ServiceDomainException {

    public ServiceNotFoundException() {
        super("Service not found.");
    }

    public ServiceNotFoundException(UUID serviceId) {
        super("Service not found with ID: " + serviceId);
    }

    public ServiceNotFoundException(String message) {
        super(message);
    }
}
