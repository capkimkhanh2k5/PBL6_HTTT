package com.danasea.backend.security.authentication.domain.exceptions;



public class OtpRequestTooFrequentException extends RuntimeException {
    public OtpRequestTooFrequentException() {
        super("OTP request is too frequent. Please wait before requesting a new OTP.");
    }
}
