package com.danasea.backend.modules.admin.domain.exceptions;

public class SelfLockNotAllowedException extends RuntimeException {
    public SelfLockNotAllowedException(String message) {
        super(message);
    }
}
