package com.danasea.backend.modules.admin.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.danasea.backend.modules.admin.domain.exception.SelfLockNotAllowedException;
import com.danasea.backend.modules.admin.domain.exception.UserAlreadyLockedException;
import com.danasea.backend.modules.admin.domain.exception.UserAlreadyUnlockedException;
import com.danasea.backend.modules.admin.domain.exception.UserNotFoundException;
import com.danasea.backend.security.authorization.domain.exception.AccessDeniedException;
import com.danasea.backend.shared.presentation.ErrorResponse;

@RestControllerAdvice
public class AdminExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(SelfLockNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleSelfLockNotAllowed(SelfLockNotAllowedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("SELF_LOCK_NOT_ALLOWED", ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyLockedException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyLocked(UserAlreadyLockedException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("USER_ALREADY_LOCKED", ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyUnlockedException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyUnlocked(UserAlreadyUnlockedException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("USER_ALREADY_UNLOCKED", ex.getMessage()));
    }

    @ExceptionHandler({
            AccessDeniedException.class,
            AuthorizationDeniedException.class,
            AccessDeniedException.class,
    })
    public ResponseEntity<ErrorResponse> handleAccessDenied(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", "Access denied"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_INPUT", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }
}
