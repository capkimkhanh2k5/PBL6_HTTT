package com.danasea.backend.modules.service.domain.exceptions;

import java.util.UUID;

public class ServiceNotFoundException extends RuntimeException {

    public ServiceNotFoundException() {
        super("Service not found");
    }

    public ServiceNotFoundException(String message) {
        super(message);
    }

    public ServiceNotFoundException(UUID serviceId) {
        super("Service not found with id: " + serviceId);
    }
}
