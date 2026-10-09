package com.danasea.backend.modules.ai.infrastructure.persistence;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.danasea.backend.modules.ai.application.port.CustomerSupportNoticePort;
import com.danasea.backend.modules.ai.application.port.CustomerSupportRequestStorePort;
import com.danasea.backend.modules.ai.domain.exceptions.AiResourceNotFoundException;
import com.danasea.backend.modules.ai.domain.exceptions.AiStateConflictException;
import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiCustomerSupportRequestJpaEntity;
import com.danasea.backend.modules.ai.infrastructure.persistence.repositories.JpaAiCustomerSupportRequestRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerSupportRequestStoreAdapter implements CustomerSupportRequestStorePort {
    private final JpaAiCustomerSupportRequestRepository repository;
    private final CustomerSupportNoticePort notices;
    private final PlatformTransactionManager transactionManager;
    @Override public Optional<Request> find(UUID id, UUID ownerId) { return repository.findByIdAndOwnerId(id, ownerId).map(this::request); }
    @Override public Optional<Request> findByKey(UUID ownerId, String key) { return repository.findByOwnerIdAndIdempotencyKey(ownerId, key).map(this::request); }
    @Override public List<Request> list(UUID ownerId, int limit) {
        return repository.findByOwnerIdOrderByCreatedAtDesc(ownerId, PageRequest.of(0, Math.min(50, Math.max(1, limit)))).stream().map(this::request).toList();
    }
    @Override public Request create(UUID ownerId, UUID orderId, String kind, String message, LocalDate desiredDate, UUID desiredSlotId, String key, String hash) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        try {
            return transaction.execute(status -> {
                var entity = new AiCustomerSupportRequestJpaEntity(); entity.setOwnerId(ownerId); entity.setOrderId(orderId); entity.setKind(kind);
                entity.setMessage(message); entity.setDesiredDate(desiredDate); entity.setDesiredSlotId(desiredSlotId); entity.setStatus("WAITING_REVIEW");
                entity.setIdempotencyKey(key); entity.setRequestHash(hash);
                Request saved = request(repository.saveAndFlush(entity)); notices.created(saved); return saved;
            });
        } catch (DataIntegrityViolationException exception) {
            var replay = findByKey(ownerId, key).orElseThrow(() -> exception);
            if (!replay.requestHash().equals(hash)) throw new AiStateConflictException("Idempotency key was already used for another request");
            return replay;
        }
    }
    @Override @Transactional public Request cancel(UUID id, UUID ownerId, long expectedVersion) {
        var entity = repository.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new AiResourceNotFoundException("Support request not found"));
        if (entity.getStatus().equals("CANCELLED")) return request(entity);
        if (entity.getVersion() != expectedVersion) throw new AiStateConflictException("Support request version has changed");
        if (!Set.of("WAITING_REVIEW", "IN_REVIEW").contains(entity.getStatus())) throw new AiStateConflictException("Support request can no longer be cancelled");
        entity.setStatus("CANCELLED"); Request saved = request(repository.saveAndFlush(entity)); notices.updated(saved); return saved;
    }
    @Override public Optional<Request> findForSupport(UUID id) { return repository.findById(id).map(this::request); }
    @Override public List<Request> supportQueue(String status, int limit) {
        return repository.findByStatusOrderByCreatedAtAsc(status, PageRequest.of(0, Math.min(100, Math.max(1, limit)))).stream().map(this::request).toList();
    }
    @Override @Transactional public Request handle(UUID id, UUID adminId, long expectedVersion, String nextStatus, String responseNote) {
        var entity = repository.findById(id).orElseThrow(() -> new AiResourceNotFoundException("Support request not found"));
        if (entity.getVersion() != expectedVersion) throw new AiStateConflictException("Support request version has changed");
        boolean allowed = entity.getStatus().equals("WAITING_REVIEW") && Set.of("IN_REVIEW", "DECLINED").contains(nextStatus)
                || entity.getStatus().equals("IN_REVIEW") && Set.of("RESOLVED", "DECLINED").contains(nextStatus);
        if (!allowed) throw new AiStateConflictException("Support request transition is unavailable");
        entity.setStatus(nextStatus); entity.setResponseNote(responseNote); entity.setHandledBy(adminId); entity.setHandledAt(OffsetDateTime.now());
        Request saved = request(repository.saveAndFlush(entity)); notices.updated(saved); return saved;
    }
    private Request request(AiCustomerSupportRequestJpaEntity entity) {
        return new Request(entity.getId(), entity.getOwnerId(), entity.getOrderId(), entity.getKind(), entity.getMessage(), entity.getDesiredDate(),
                entity.getDesiredSlotId(), entity.getStatus(), entity.getVersion(), entity.getIdempotencyKey(), entity.getRequestHash(), entity.getCreatedAt(),
                entity.getUpdatedAt(), "MANUAL_SUPPORT; ORDER_AND_PAYMENT_CHANGES_REQUIRE_NATIVE_AUTHORIZED_WORKFLOWS", false, false,
                entity.getResponseNote(), entity.getHandledBy(), entity.getHandledAt());
    }
}
