package com.danasea.backend.modules.admin.domain.exception;

public class SelfLockNotAllowedException extends RuntimeException {
    public SelfLockNotAllowedException(String message) {
        super(message);
    }
}
