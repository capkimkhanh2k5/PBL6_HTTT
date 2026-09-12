package com.danasea.backend.security.authentication.domain.exceptions;



public class OtpMaxAttemptsExceededException extends RuntimeException {
    public OtpMaxAttemptsExceededException() {
        super("Maximum OTP attempts exceeded. Please request a new OTP.");
    }
}
