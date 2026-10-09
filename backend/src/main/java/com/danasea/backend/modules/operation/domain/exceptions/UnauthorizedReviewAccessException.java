package com.danasea.backend.modules.operation.domain.exceptions;

public class UnauthorizedReviewAccessException extends RuntimeException {

    public UnauthorizedReviewAccessException(String message) {
        super(message);
    }
}
