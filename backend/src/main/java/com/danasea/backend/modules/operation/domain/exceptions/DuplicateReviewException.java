package com.danasea.backend.modules.operation.domain.exceptions;

public class DuplicateReviewException extends RuntimeException {

    public DuplicateReviewException(String message) {
        super(message);
    }
}
