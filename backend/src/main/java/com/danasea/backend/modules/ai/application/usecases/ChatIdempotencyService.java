package com.danasea.backend.modules.ai.application.usecases;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.danasea.backend.modules.ai.domain.exceptions.AiStateConflictException;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiChatRequestJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiChatRequestRepository;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ChatIdempotencyService {
    public record Claim(UUID id, UUID conversationId, Map<String, Object> response) {
        public boolean replay() { return response != null; }
    }

    private final JpaAiChatRequestRepository requests;
    private final ObjectMapper mapper;
    private final TransactionTemplate transactions;

    public ChatIdempotencyService(JpaAiChatRequestRepository requests, ObjectMapper mapper,
            PlatformTransactionManager transactionManager) {
        this.requests = requests;
        this.mapper = mapper;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public Claim begin(UUID actor, String key, UUID requestedConversation, String message, SupportedLanguage language) {
        if (actor == null) throw new IllegalArgumentException("Authenticate before using an idempotency key");
        if (key == null || !key.matches("[!-~]{1,128}")) throw new IllegalArgumentException("Invalid idempotency key");
        String fingerprint = fingerprint(requestedConversation + "\n" + language.code() + "\n" + message);
        var previous = requests.findByActorIdAndRequestKey(actor, key);
        if (previous.isPresent()) return replay(previous.get(), fingerprint);
        try {
            return transactions.execute(status -> {
                AiChatRequestJpaEntity request = new AiChatRequestJpaEntity();
                request.setActorId(actor); request.setRequestKey(key); request.setRequestFingerprint(fingerprint);
                request.setStatus("PROCESSING"); request.setExpiresAt(OffsetDateTime.now().plusDays(1));
                requests.saveAndFlush(request);
                return new Claim(request.getId(), null, null);
            });
        } catch (DataIntegrityViolationException duplicate) {
            return replay(requests.findByActorIdAndRequestKey(actor, key).orElseThrow(() -> duplicate), fingerprint);
        }
    }

    public void bind(Claim claim, UUID conversationId) {
        if (claim == null) return;
        transactions.executeWithoutResult(status -> {
            var entity = requests.findById(claim.id()).orElseThrow();
            entity.setConversationId(conversationId); requests.save(entity);
        });
    }

    public void complete(Claim claim, Map<String, Object> response) {
        if (claim == null) return;
        String json;
        try { json = mapper.writeValueAsString(response); }
        catch (Exception exception) { throw new IllegalStateException("Unable to serialize chat outcome", exception); }
        transactions.executeWithoutResult(status -> {
            var entity = requests.findById(claim.id()).orElseThrow();
            entity.setStatus("COMPLETE"); entity.setResponseJson(json); requests.save(entity);
        });
    }

    public void fail(Claim claim) {
        if (claim == null) return;
        transactions.executeWithoutResult(status -> requests.findById(claim.id()).ifPresent(entity -> {
            entity.setStatus("FAILED"); requests.save(entity);
        }));
    }

    private Claim replay(AiChatRequestJpaEntity request, String fingerprint) {
        if (!request.getRequestFingerprint().equals(fingerprint))
            throw new AiStateConflictException("Idempotency key was already used for another chat request");
        if (!"COMPLETE".equals(request.getStatus()))
            throw new AiStateConflictException("The chat request is processing or requires recovery");
        try {
            return new Claim(request.getId(), request.getConversationId(), mapper.readValue(request.getResponseJson(),
                    new TypeReference<Map<String, Object>>() { }));
        } catch (Exception exception) { throw new IllegalStateException("Unable to read chat outcome", exception); }
    }

    private String fingerprint(String input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }
}
