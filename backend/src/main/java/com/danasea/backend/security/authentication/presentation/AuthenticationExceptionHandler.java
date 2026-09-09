package com.danasea.backend.security.authentication.presentation;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.danasea.backend.security.authentication.domain.exception.EmailAlreadyUsedException;
import com.danasea.backend.security.authentication.domain.exception.InvalidCredentialsException;
import com.danasea.backend.security.authentication.domain.exception.OtpExpiredException;
import com.danasea.backend.security.authentication.domain.exception.OtpInvalidException;
import com.danasea.backend.security.authentication.domain.exception.OtpMaxAttemptsExceededException;
import com.danasea.backend.security.authentication.domain.exception.OtpRequestTooFrequentException;
import com.danasea.backend.security.authentication.infrastructure.security.CookieUtils;
import com.danasea.backend.shared.presentation.ErrorResponse;

@RestControllerAdvice
public class AuthenticationExceptionHandler {

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleInvalidCredentials(
                        InvalidCredentialsException exception,
                        HttpServletResponse response) {

                CookieUtils.clearRefreshTokenCookie(response);

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(new ErrorResponse(
                                                "INVALID_CREDENTIALS",
                                                exception.getMessage()));
        }

        @ExceptionHandler(EmailAlreadyUsedException.class)
        public ResponseEntity<ErrorResponse> handleEmailAlreadyUsed(
                        EmailAlreadyUsedException exception) {
                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(new ErrorResponse(
                                                "EMAIL_ALREADY_USED",
                                                exception.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException exception) {

                String message = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .findFirst()
                                .orElse("Invalid request");

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(new ErrorResponse(
                                                "INVALID_INPUT",
                                                message));
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
