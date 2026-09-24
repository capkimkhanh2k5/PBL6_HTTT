package com.danasea.backend.shared.presentation;

import com.danasea.backend.shared.i18n.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Locale;
import java.util.stream.Collectors;

public abstract class LocalizedExceptionHandlerSupport {

    private LocalizedMessageService messages = LocalizedMessageService.standalone();

    @Autowired(required = false)
    void setLocalizedMessageService(LocalizedMessageService messages) {
        this.messages = messages;
    }

    protected final ErrorResponse error(String code) {
        return new ErrorResponse(code, messageForCode(code));
    }

    protected final ErrorResponse validationError(MethodArgumentNotValidException exception) {
        return new ErrorResponse("INVALID_INPUT", validationMessage(exception));
    }

    protected final String validationMessage(MethodArgumentNotValidException exception) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return details.isBlank() ? messages.get("error.invalid_input") : details;
    }

    protected final String message(String key, Object... args) {
        return messages.get(key, args);
    }

    private String messageForCode(String code) {
        String key = "error." + code.toLowerCase(Locale.ROOT);
        return messages.getOrDefault(key, messages.get("error.internal"));
    }
}
