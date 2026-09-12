package com.danasea.backend.security.authentication.domain.exceptions;



public class OtpExpiredException extends RuntimeException {
    public OtpExpiredException() {
        super("OTP has expired or does not exist.");
    }
}
