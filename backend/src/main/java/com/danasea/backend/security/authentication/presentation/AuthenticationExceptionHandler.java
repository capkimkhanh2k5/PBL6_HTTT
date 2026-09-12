package com.danasea.backend.security.authentication.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.danasea.backend.security.authentication.domain.exceptions.EmailAlreadyUsedException;
import com.danasea.backend.security.authentication.domain.exceptions.OtpExpiredException;
import com.danasea.backend.security.authentication.domain.exceptions.OtpInvalidException;
import com.danasea.backend.security.authentication.domain.exceptions.OtpMaxAttemptsExceededException;
import com.danasea.backend.security.authentication.domain.exceptions.OtpRequestTooFrequentException;
import com.danasea.backend.shared.presentation.ErrorResponse;

@RestControllerAdvice(basePackageClasses = AuthenticationController.class)
public class AuthenticationExceptionHandler {

        @ExceptionHandler(EmailAlreadyUsedException.class)
        public ResponseEntity<ErrorResponse> handleEmailAlreadyUsed(
                        EmailAlreadyUsedException exception) {
                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(new ErrorResponse(
                                                "EMAIL_ALREADY_USED",
                                                exception.getMessage()));
        }

        @ExceptionHandler({OtpInvalidException.class, OtpExpiredException.class, OtpMaxAttemptsExceededException.class})
        public ResponseEntity<ErrorResponse> handleOtpVerificationExceptions(Exception exception) {
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(new ErrorResponse(
                                                "INVALID_OTP",
                                                "The OTP provided is invalid, expired, or has exceeded maximum attempts."));
        }

        @ExceptionHandler(OtpRequestTooFrequentException.class)
        public ResponseEntity<ErrorResponse> handleOtpTooFrequent(OtpRequestTooFrequentException exception) {
                return ResponseEntity
                                .status(HttpStatus.TOO_MANY_REQUESTS)
                                .body(new ErrorResponse(
                                                "OTP_REQUEST_TOO_FREQUENT",
                                                exception.getMessage()));
        }
}
