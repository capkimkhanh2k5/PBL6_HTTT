package com.danasea.backend.modules.operation.domain.exceptions;

public class InvalidReviewSubOrderStateException extends RuntimeException {

    public InvalidReviewSubOrderStateException(String message) {
        super(message);
    }
}
