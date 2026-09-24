package com.danasea.backend.modules.dispute.domain.exceptions;

public class InvalidDisputeConditionException extends InvalidSubOrderStateException {
    public InvalidDisputeConditionException(String message) {
        super(message);
    }
}
