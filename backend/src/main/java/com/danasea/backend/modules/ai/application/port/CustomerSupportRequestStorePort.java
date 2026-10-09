package com.danasea.backend.modules.ai.application.port;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerSupportRequestStorePort {
    record Request(UUID id, UUID ownerId, UUID orderId, String kind, String message, LocalDate desiredDate,
                   UUID desiredSlotId, String status, long version, String idempotencyKey, String requestHash,
                   OffsetDateTime createdAt, OffsetDateTime updatedAt, String processingMode,
                   boolean financialActionPerformed, boolean inventoryReserved, String responseNote, UUID handledBy, OffsetDateTime handledAt) {}
    Optional<Request> find(UUID id, UUID ownerId);
    Optional<Request> findByKey(UUID ownerId, String key);
    List<Request> list(UUID ownerId, int limit);
    Request create(UUID ownerId, UUID orderId, String kind, String message, LocalDate desiredDate,
                   UUID desiredSlotId, String idempotencyKey, String requestHash);
    Request cancel(UUID id, UUID ownerId, long expectedVersion);
    Optional<Request> findForSupport(UUID id);
    List<Request> supportQueue(String status, int limit);
    Request handle(UUID id, UUID adminId, long expectedVersion, String status, String responseNote);
}
