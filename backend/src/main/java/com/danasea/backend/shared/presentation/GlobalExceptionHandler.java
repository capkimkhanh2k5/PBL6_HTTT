package com.danasea.backend.shared.presentation;

import com.danasea.backend.security.authorization.domain.exceptions.AccessDeniedException;
import com.danasea.backend.security.authentication.domain.exceptions.InvalidCredentialsException;
import com.danasea.backend.security.authentication.infrastructure.security.CookieUtils;
import com.danasea.backend.shared.i18n.LocalizedException;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.modules.ai.domain.exceptions.AiConversationLocaleMismatchException;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final LocalizedMessageService messages;

    public GlobalExceptionHandler(LocalizedMessageService messages) {
        this.messages = messages;
    }

    public GlobalExceptionHandler() {
        this(LocalizedMessageService.standalone());
    }

    @ExceptionHandler(LocalizedException.class)
    public ResponseEntity<ErrorResponse> handleLocalizedException(LocalizedException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getErrorCode(), messages.get(exception.getMessageRef())));
    }

    @ExceptionHandler(AiConversationLocaleMismatchException.class)
    public ResponseEntity<ErrorResponse> handleAiConversationLocaleMismatch(
            AiConversationLocaleMismatchException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(exception.getErrorCode(), messages.get(exception.getMessageRef())));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletResponse response) {
                
        CookieUtils.clearRefreshTokenCookie(response);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", messages.get("error.invalid_credentials")));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", messages.get("error.access_denied")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_INPUT", messages.get("error.invalid_input")));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_INPUT", message));
    }
}
