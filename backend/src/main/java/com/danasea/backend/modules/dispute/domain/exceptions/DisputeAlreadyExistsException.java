package com.danasea.backend.modules.dispute.domain.exceptions;

public class DisputeAlreadyExistsException extends DuplicateDisputeException {
    public DisputeAlreadyExistsException(String message) {
        super(message);
    }
}
