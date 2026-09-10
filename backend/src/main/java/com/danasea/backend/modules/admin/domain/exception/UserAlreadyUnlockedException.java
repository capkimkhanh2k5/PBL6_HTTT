package com.danasea.backend.modules.admin.domain.exception;

public class UserAlreadyUnlockedException extends RuntimeException {
    public UserAlreadyUnlockedException(String message) {
        super(message);
    }
}
