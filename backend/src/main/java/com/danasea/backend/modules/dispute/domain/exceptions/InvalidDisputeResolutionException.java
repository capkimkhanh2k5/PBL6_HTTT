package com.danasea.backend.modules.dispute.domain.exceptions;

public class InvalidDisputeResolutionException extends RuntimeException {
    public InvalidDisputeResolutionException(String message) {
        super(message);
    }
}
