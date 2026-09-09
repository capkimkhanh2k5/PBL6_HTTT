package com.danasea.backend.security.authentication.domain.exception;



public class OtpExpiredException extends RuntimeException {
    public OtpExpiredException() {
        super("OTP has expired or does not exist.");
    }
}
