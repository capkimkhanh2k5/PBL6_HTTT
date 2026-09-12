package com.danasea.backend.security.authentication.domain.exceptions;



public class OtpInvalidException extends RuntimeException {
    public OtpInvalidException() {
        super("Invalid OTP.");
    }
}
