package com.danasea.backend.modules.admin.domain.exceptions;

public class UserAlreadyLockedException extends RuntimeException {
    public UserAlreadyLockedException(String message) {
        super(message);
    }
}
