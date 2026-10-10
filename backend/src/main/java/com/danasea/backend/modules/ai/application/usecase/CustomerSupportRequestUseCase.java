package com.danasea.backend.modules.ai.application.usecase;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.ai.application.port.CustomerOrderReadPort;
import com.danasea.backend.modules.ai.application.port.CustomerSupportRequestStorePort;
import com.danasea.backend.modules.ai.application.port.CustomerSupportRequestStorePort.Request;
import com.danasea.backend.modules.ai.domain.exceptions.AiResourceNotFoundException;
import com.danasea.backend.modules.ai.domain.exceptions.AiStateConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerSupportRequestUseCase {
    public record Command(UUID orderId, String kind, String message, LocalDate desiredDate, UUID desiredSlotId, Boolean confirmed) {}
    public record Preview(UUID orderId, String kind, boolean requestAvailable, String processingMode,
                          List<String> reasonCodes, OffsetDateTime checkedAt, boolean financialActionPerformed, boolean inventoryReserved) {}
    private final CustomerOrderReadPort orders;
    private final CustomerSupportRequestStorePort store;
    private final ObjectMapper mapper;

    public Preview preview(UUID ownerId, Command command) {
        validate(ownerId, command);
        orders.read(ownerId, command.orderId());
        var eligibility = orders.eligibility(ownerId, command.orderId(), null);
        boolean available = command.kind().equals("HANDOFF") || eligibility != null && eligibility.changeRequestAvailable();
        if (command.kind().equals("CHANGE_REQUEST")) orders.validateChangeTarget(ownerId, command.orderId(), command.desiredSlotId(), command.desiredDate());
        return new Preview(command.orderId(), command.kind(), available, "HUMAN_REVIEW_REQUIRED; NO_AUTOMATIC_RESCHEDULE",
                available ? List.of("ORDER_AND_PAYMENT_UNCHANGED", "PRICE_AND_CAPACITY_REQUIRE_HUMAN_REVALIDATION") : List.of("CHANGE_UNAVAILABLE_FOR_CURRENT_ORDER_STATE"),
                OffsetDateTime.now(), false, false);
    }
    public Request create(UUID ownerId, Command command, String key) {
        validate(ownerId, command); CustomerPreferenceUseCase.validateKey(key);
        if (!Boolean.TRUE.equals(command.confirmed())) throw new IllegalArgumentException("Explicit customer confirmation is required");
        String hash = hash(command);
        var replay = store.findByKey(ownerId, key);
        if (replay.isPresent()) {
            if (!replay.get().requestHash().equals(hash)) throw new AiStateConflictException("Idempotency key was already used for another request");
            return replay.get();
        }
        Preview preview = preview(ownerId, command);
        if (!preview.requestAvailable()) throw new AiStateConflictException("Change request is unavailable for current order state");
        return store.create(ownerId, command.orderId(), command.kind(), command.message().strip(), command.desiredDate(), command.desiredSlotId(), key, hash);
    }
    public List<Request> list(UUID ownerId) { requireOwner(ownerId); return store.list(ownerId, 50); }
    public Request get(UUID ownerId, UUID id) {
        requireOwner(ownerId); return store.find(id, ownerId).orElseThrow(() -> new AiResourceNotFoundException("Support request not found"));
    }
    public Request cancel(UUID ownerId, UUID id, Long expectedVersion) {
        requireOwner(ownerId);
        if (expectedVersion == null || expectedVersion < 0) throw new IllegalArgumentException("expectedVersion is required");
        return store.cancel(id, ownerId, expectedVersion);
    }
    public List<Request> supportQueue(String status, int limit) {
        if (status == null || !Set.of("WAITING_REVIEW", "IN_REVIEW", "RESOLVED", "DECLINED", "CANCELLED").contains(status) || limit < 1 || limit > 100) throw new IllegalArgumentException("A valid support status and limit between 1 and 100 are required");
        return store.supportQueue(status, limit);
    }
    public Request getForSupport(UUID id) { return store.findForSupport(id).orElseThrow(() -> new AiResourceNotFoundException("Support request not found")); }
    public Request handle(UUID adminId, UUID id, Long expectedVersion, String status, String responseNote) {
        requireOwner(adminId);
        if (id == null || expectedVersion == null || expectedVersion < 0 || status == null || !Set.of("IN_REVIEW", "RESOLVED", "DECLINED").contains(status)
                || responseNote == null || responseNote.isBlank() || responseNote.length() > 1800) throw new IllegalArgumentException("A valid support transition, expectedVersion and customer response note are required");
        return store.handle(id, adminId, expectedVersion, status, responseNote.strip());
    }
    private void validate(UUID ownerId, Command command) {
        requireOwner(ownerId);
        if (command == null || command.orderId() == null || command.kind() == null || !Set.of("HANDOFF", "CHANGE_REQUEST").contains(command.kind())
                || command.message() == null || command.message().isBlank() || command.message().length() > 1800) throw new IllegalArgumentException("An owned order, valid request kind and message of 1 to 1800 characters are required");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (command.desiredDate() != null && (command.desiredDate().isBefore(today) || command.desiredDate().isAfter(today.plusDays(90)))) throw new IllegalArgumentException("Requested date must be within the next ninety days");
        if (command.kind().equals("HANDOFF") && (command.desiredSlotId() != null || command.desiredDate() != null)) throw new IllegalArgumentException("A handoff must not specify a reschedule target");
        if (command.kind().equals("CHANGE_REQUEST") && command.desiredSlotId() == null && command.desiredDate() == null) throw new IllegalArgumentException("A requested date or slot is required for a change request");
    }
    private String hash(Command command) {
        try { return CustomerPreferenceUseCase.hash(mapper.writeValueAsString(command)); }
        catch (Exception exception) { throw new IllegalStateException("Cannot fingerprint support request", exception); }
    }
    private void requireOwner(UUID ownerId) { if (ownerId == null) throw new IllegalArgumentException("An authenticated customer is required"); }
}
