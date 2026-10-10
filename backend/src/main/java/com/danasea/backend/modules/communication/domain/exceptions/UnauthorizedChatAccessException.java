package com.danasea.backend.modules.communication.domain.exceptions;

import org.springframework.security.access.AccessDeniedException;

public class UnauthorizedChatAccessException extends AccessDeniedException {

    public UnauthorizedChatAccessException(String message) {
        super(message);
    }
}
