package com.danasea.backend.modules.vendor.domain.exception;

public class UserLockedException extends RuntimeException {

    public UserLockedException() {
        super("User account is locked");
    }

    public UserLockedException(String message) {
        super(message);
    }
}
