package com.danasea.backend.modules.ai.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.ai.application.ports.ConfirmationOutcomePort;
import com.danasea.backend.modules.ai.domain.exceptions.AiStateConflictException;
import com.danasea.backend.modules.ai.domain.models.ConfirmationCard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConfirmationOutcomeAdapter implements ConfirmationOutcomePort {
    private record Outcome(UUID owner, UUID conversation, String fingerprint, String state, String response) { }
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> completed(String cardId, UUID ownerId, UUID conversationId) {
        requireArguments(cardId, ownerId, conversationId);
        var stored = lookup(cardId, false);
        if (stored.isEmpty()) return Optional.empty();
        requireBinding(stored.get(), ownerId, conversationId);
        return stored.get().response() == null ? Optional.empty() : Optional.of(response(stored.get().response()));
    }

    @Override
    @Transactional(timeout = 20)
    public Map<String, Object> execute(ConfirmationCard card, UUID ownerId, UUID conversationId,
            Supplier<Map<String, Object>> action) {
        requireArguments(card.getId(), ownerId, conversationId);
        jdbc.execute("set local lock_timeout = '5s'");
        if (!conversationId.equals(card.getConversationId()) || card.getOwnerId() != null && !ownerId.equals(card.getOwnerId()))
            throw new AccessDeniedException("Confirmation card does not belong to this customer and conversation");
        UUID actualOwner = jdbc.query("select user_id from ai_conversations where id=?", (result, row) ->
                result.getObject(1, UUID.class), conversationId).stream().findFirst().orElse(null);
        if (!ownerId.equals(actualOwner)) throw new AccessDeniedException("Conversation does not belong to this customer");
        String fingerprint = fingerprint(card);
        jdbc.update("insert into ai_confirmation_outcomes(card_id,owner_id,conversation_id,card_fingerprint,state) values(?,?,?,?,'PROCESSING') on conflict(card_id) do nothing",
                card.getId(), ownerId, conversationId, fingerprint);
        Outcome locked = lookup(card.getId(), true).orElseThrow();
        requireBinding(locked, ownerId, conversationId);
        if (!locked.fingerprint().equals(fingerprint)) throw new AiStateConflictException("Confirmation card details changed for an existing card ID");
        if (locked.response() != null) return response(locked.response());
        if (!ConfirmationCard.STATUS_PENDING.equals(card.getStatus()))
            throw new AiStateConflictException("The cached confirmation state requires recovery; no new hold was created");

        Map<String, Object> outcome = action.get();
        boolean success = "success".equals(outcome.get("status"));
        UUID bookingId = success ? bookingId(outcome.get("bookingId")) : null;
        if (success) {
            entityManager.flush();
            UUID customer = jdbc.query("select customer_id from bookings where id=?", (result, row) ->
                    result.getObject(1, UUID.class), bookingId).stream().findFirst().orElse(null);
            if (!ownerId.equals(customer)) throw new IllegalStateException("Native hold outcome does not belong to this customer");
        }
        jdbc.update("update ai_confirmation_outcomes set state=?,booking_id=?,response_json=?,updated_at=current_timestamp where card_id=?",
                success ? "SUCCESS" : "NO_HOLD", bookingId, json(outcome), card.getId());
        return outcome;
    }

    private Optional<Outcome> lookup(String cardId, boolean lock) {
        return jdbc.query("select owner_id,conversation_id,card_fingerprint,state,response_json from ai_confirmation_outcomes where card_id=?"
                + (lock ? " for update" : ""), (result, row) -> new Outcome(result.getObject(1, UUID.class),
                        result.getObject(2, UUID.class), result.getString(3), result.getString(4), result.getString(5)), cardId)
                .stream().findFirst();
    }

    private void requireArguments(String cardId, UUID ownerId, UUID conversationId) {
        if (cardId == null || !cardId.matches("[A-Za-z0-9._:-]{1,128}")) throw new IllegalArgumentException("A valid confirmation card ID is required");
        if (ownerId == null || conversationId == null) throw new AccessDeniedException("An authenticated conversation is required");
    }

    private void requireBinding(Outcome outcome, UUID owner, UUID conversation) {
        if (!owner.equals(outcome.owner()) || !conversation.equals(outcome.conversation()))
            throw new AccessDeniedException("Confirmation outcome does not belong to this customer and conversation");
    }

    private String fingerprint(ConfirmationCard card) {
        // Status and retry-cache counters can change; the offered quote and binding cannot.
        String immutable = card.getId() + "|" + card.getConversationId() + "|" + card.getServiceId() + "|" + card.getOptionId()
                + "|" + card.getSlotId() + "|" + card.getParticipantsCount() + "|" + card.getQuantity() + "|"
                + card.getParticipantsPerPackage() + "|"
                + (card.getPrice() == null ? "null" : card.getPrice().stripTrailingZeros().toPlainString())
                + "|" + card.getDate() + "|" + card.getLocale();
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(immutable.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("Unable to fingerprint confirmation card", exception); }
    }

    private UUID bookingId(Object value) {
        try { return value instanceof UUID id ? id : UUID.fromString(String.valueOf(value)); }
        catch (RuntimeException exception) { throw new IllegalStateException("A successful native confirmation must reference its hold", exception); }
    }

    private String json(Map<String, Object> outcome) {
        try { return mapper.writeValueAsString(outcome); }
        catch (Exception exception) { throw new IllegalStateException("Unable to persist native confirmation outcome", exception); }
    }

    private Map<String, Object> response(String json) {
        try { return mapper.readValue(json, new TypeReference<Map<String, Object>>() { }); }
        catch (Exception exception) { throw new IllegalStateException("Unable to read native confirmation outcome", exception); }
    }
}
