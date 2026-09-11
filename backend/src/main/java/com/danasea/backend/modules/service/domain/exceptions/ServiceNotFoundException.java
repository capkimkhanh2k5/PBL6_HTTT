package com.danasea.backend.modules.service.domain.exceptions;

public class ServiceNotFoundException extends RuntimeException {

    public ServiceNotFoundException() {
        super("Service not found.");
    }

    public ServiceNotFoundException(String message) {
        super(message);
    }
}
