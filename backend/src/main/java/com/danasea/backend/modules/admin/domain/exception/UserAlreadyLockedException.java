package com.danasea.backend.modules.admin.domain.exception;

public class UserAlreadyLockedException extends RuntimeException {
    public UserAlreadyLockedException(String message) {
        super(message);
    }
}
