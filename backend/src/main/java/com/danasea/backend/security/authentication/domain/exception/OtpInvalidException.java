package com.danasea.backend.security.authentication.domain.exception;



public class OtpInvalidException extends RuntimeException {
    public OtpInvalidException() {
        super("Invalid OTP.");
    }
}
