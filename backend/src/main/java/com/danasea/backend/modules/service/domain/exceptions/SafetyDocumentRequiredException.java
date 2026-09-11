package com.danasea.backend.modules.service.domain.exceptions;

public class SafetyDocumentRequiredException extends RuntimeException {

    public SafetyDocumentRequiredException() {
        super("High-risk services require an approved safety document before publishing.");
    }

    public SafetyDocumentRequiredException(String message) {
        super(message);
    }
}
