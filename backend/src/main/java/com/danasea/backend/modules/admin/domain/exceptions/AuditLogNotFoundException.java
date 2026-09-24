package com.danasea.backend.modules.admin.domain.exceptions;

public class AuditLogNotFoundException extends RuntimeException {
    public AuditLogNotFoundException(String message) {
        super(message);
    }
}
