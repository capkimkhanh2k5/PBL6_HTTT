package com.danasea.backend.modules.service.application.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.shared.i18n.SupportedLanguage;

public interface AiCatalogReadApi {
    record Query(String keyword, UUID categoryId, LocalDate from, LocalDate to, int partySize,
                 Double latitude, Double longitude, Double radiusKm, int limit, SupportedLanguage language) {}
    record Option(UUID id, String name, String pricingUnit, BigDecimal unitPrice, int quantity,
                  BigDecimal partyTotal, String benefits, List<Slot> slots) {}
    record Slot(UUID id, LocalDate date, LocalTime start, LocalTime end, int availableQuantity) {}
    record PublishedService(UUID id, UUID vendorId, String name, String description, UUID categoryId,
                            String categoryName, String categorySlug, String address, Double latitude,
                            Double longitude, Integer durationMinutes, boolean weatherSensitive,
                            BigDecimal averageRating, int reviewCount, List<Option> options) {}

    List<PublishedService> search(Query query);
    PublishedService find(UUID id, Query query);
    PublishedService metadata(UUID id, SupportedLanguage language);
}
