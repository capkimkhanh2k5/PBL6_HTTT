package com.danasea.backend.modules.admin.domain.exceptions;

public class UserAlreadyUnlockedException extends RuntimeException {
    public UserAlreadyUnlockedException(String message) {
        super(message);
    }
}
