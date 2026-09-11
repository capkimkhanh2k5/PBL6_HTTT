package com.danasea.backend.modules.service.domain.exceptions;

import com.danasea.backend.modules.service.domain.models.ServiceStatus;

public class InvalidServiceStateException extends ServiceDomainException {

    public InvalidServiceStateException(String message) {
        super(message);
    }

    public InvalidServiceStateException(ServiceStatus status, String action) {
        super("Cannot perform " + action + " on service in status " + status);
    }
}
