package com.danasea.backend.modules.communication.domain.exceptions;

import java.util.UUID;

public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(UUID id) {
        super("Conversation not found with id: " + id);
    }

    public ConversationNotFoundException(String message) {
        super(message);
    }
}
