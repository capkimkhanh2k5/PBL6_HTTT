package com.danasea.backend.security.authentication.domain.exception;



public class OtpMaxAttemptsExceededException extends RuntimeException {
    public OtpMaxAttemptsExceededException() {
        super("Maximum OTP attempts exceeded. Please request a new OTP.");
    }
}
