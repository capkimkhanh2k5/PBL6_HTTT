package com.danasea.backend.modules.ai.domain.exceptions;

public class AiStateConflictException extends RuntimeException {
    public AiStateConflictException(String message) { super(message); }
}
