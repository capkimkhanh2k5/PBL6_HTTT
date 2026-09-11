package com.danasea.backend.modules.service.domain.exceptions;

public class ServiceDomainException extends RuntimeException {

    public ServiceDomainException(String message) {
        super(message);
    }

    public ServiceDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
