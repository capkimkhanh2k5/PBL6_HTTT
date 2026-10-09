package com.danasea.backend.modules.ai.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ItineraryPlan(String status, TravelContext context, BigDecimal totalPrice, List<Item> items,
                            List<String> unresolvedConstraints, String planningMethod, boolean inventoryReserved) {
    public record Item(UUID serviceId, UUID optionId, UUID slotId, String serviceName,
                       LocalDateTime start, LocalDateTime end, int quantity, BigDecimal unitPrice,
                       BigDecimal partyTotal, Double latitude, Double longitude, int estimatedTravelMinutes,
                       String weatherStatus, boolean provisionalWeather, String weatherWarning) {}
    public record Saved(UUID id, UUID ownerId, long version, ItineraryPlan plan,
                        OffsetDateTime createdAt, OffsetDateTime updatedAt, String lifecycle,
                        boolean bookingChanged, String locale) {
        public Saved(UUID id, UUID ownerId, long version, ItineraryPlan plan,
                     OffsetDateTime createdAt, OffsetDateTime updatedAt) {
            this(id, ownerId, version, plan, createdAt, updatedAt, "DRAFT", false, "vi");
        }
        public Saved(UUID id, UUID ownerId, long version, ItineraryPlan plan,
                     OffsetDateTime createdAt, OffsetDateTime updatedAt, String lifecycle, boolean bookingChanged) {
            this(id, ownerId, version, plan, createdAt, updatedAt, lifecycle, bookingChanged, "vi");
        }
    }
    public record Alternative(String id, ItineraryPlan plan, List<String> tradeOffs) {}
    public record Preview(UUID id, List<Alternative> alternatives, OffsetDateTime createdAt,
                          OffsetDateTime expiresAt, boolean inventoryReserved) {}
    public record Proposal(UUID id, UUID itineraryId, long baseVersion, String state,
                           ItineraryPlan plan, List<UUID> removedSlotIds, List<UUID> addedSlotIds,
                           List<Item> changedItems, String trigger, String sourceEventId,
                           OffsetDateTime createdAt, boolean bookingChanged) {}
    public record Revision(long version, String lifecycle, ItineraryPlan plan, String reason,
                           UUID proposalId, OffsetDateTime createdAt) {}
}
