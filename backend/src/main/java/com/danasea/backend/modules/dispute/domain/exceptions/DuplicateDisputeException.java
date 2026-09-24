package com.danasea.backend.modules.dispute.domain.exceptions;

public class DuplicateDisputeException extends RuntimeException {
    public DuplicateDisputeException(String message) {
        super(message);
    }
}
