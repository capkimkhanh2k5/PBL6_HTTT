package com.danasea.backend.modules.ai.application.port;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.danasea.backend.modules.ai.domain.models.ConfirmationCard;

public interface ConfirmationOutcomePort {
    Optional<Map<String, Object>> completed(String cardId, UUID ownerId, UUID conversationId);

    /** Executes the canonical hold and records its outcome in one database transaction. */
    Map<String, Object> execute(ConfirmationCard card, UUID ownerId, UUID conversationId,
            Supplier<Map<String, Object>> action);
}
