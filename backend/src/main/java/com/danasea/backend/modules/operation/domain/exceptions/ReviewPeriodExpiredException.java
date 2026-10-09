package com.danasea.backend.modules.operation.domain.exceptions;

public class ReviewPeriodExpiredException extends RuntimeException {

    public ReviewPeriodExpiredException(String message) {
        super(message);
    }
}
