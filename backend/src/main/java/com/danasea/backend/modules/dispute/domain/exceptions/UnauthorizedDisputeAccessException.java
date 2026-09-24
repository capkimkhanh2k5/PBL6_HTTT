package com.danasea.backend.modules.dispute.domain.exceptions;

import org.springframework.security.access.AccessDeniedException;

public class UnauthorizedDisputeAccessException extends AccessDeniedException {
    public UnauthorizedDisputeAccessException(String message) {
        super(message);
    }
}
