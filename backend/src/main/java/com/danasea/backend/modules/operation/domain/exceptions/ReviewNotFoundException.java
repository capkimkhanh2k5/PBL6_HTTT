package com.danasea.backend.modules.operation.domain.exceptions;

import java.util.UUID;

public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(String message) {
        super(message);
    }

    public ReviewNotFoundException(UUID reviewId) {
        super("Review not found with id: " + reviewId);
    }
}
