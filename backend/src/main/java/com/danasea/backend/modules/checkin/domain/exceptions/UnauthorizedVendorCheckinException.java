package com.danasea.backend.modules.checkin.domain.exceptions;

public class UnauthorizedVendorCheckinException extends RuntimeException {
    public UnauthorizedVendorCheckinException(String message) {
        super(message);
    }

    public UnauthorizedVendorCheckinException(String message, Throwable cause) {
        super(message, cause);
    }
}
