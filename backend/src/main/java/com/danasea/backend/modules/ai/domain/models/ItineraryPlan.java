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
                        OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
}
