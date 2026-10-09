package com.danasea.backend.modules.service.application.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.shared.i18n.SupportedLanguage;

public interface AiCatalogReadApi {
    record Query(String keyword, UUID categoryId, LocalDate from, LocalDate to, int partySize,
                 Double latitude, Double longitude, Double radiusKm, int limit, SupportedLanguage language,
                 BigDecimal totalBudget, LocalTime dayStart, LocalTime dayEnd, List<String> includedActivities,
                 List<String> excludedActivities, String mode, int offset, List<String> rankingInterests) {
        public Query(String keyword, UUID categoryId, LocalDate from, LocalDate to, int partySize,
                     Double latitude, Double longitude, Double radiusKm, int limit, SupportedLanguage language) {
            this(keyword, categoryId, from, to, partySize, latitude, longitude, radiusKm, limit, language,
                    null, null, null, List.of(), List.of(), "SEARCH", 0, List.of());
        }
    }
    record Option(UUID id, String name, String pricingUnit, BigDecimal unitPrice, int quantity,
                  BigDecimal partyTotal, String benefits, List<Slot> slots, Integer maxPaxPerPackage) {
        public Option(UUID id, String name, String pricingUnit, BigDecimal unitPrice, int quantity,
                      BigDecimal partyTotal, String benefits, List<Slot> slots) {
            this(id, name, pricingUnit, unitPrice, quantity, partyTotal, benefits, slots, null);
        }
    }
    record Slot(UUID id, LocalDate date, LocalTime start, LocalTime end, int availableQuantity) {}
    record PublishedService(UUID id, UUID vendorId, String name, String description, UUID categoryId,
                            String categoryName, String categorySlug, String address, Double latitude,
                            Double longitude, Integer durationMinutes, boolean weatherSensitive,
                            BigDecimal averageRating, int reviewCount, List<Option> options) {}

    record SearchPage(List<PublishedService> services, Integer nextOffset, boolean exhausted,
                      int inspectedCandidates, List<String> limitations) {}
    List<PublishedService> search(Query query);
    default SearchPage searchPage(Query query) {
        List<PublishedService> items = search(query);
        return new SearchPage(items, items.size() >= query.limit() ? query.offset() + items.size() : null,
                false, items.size(), List.of("LEGACY_RETRIEVAL_COVERAGE_UNKNOWN"));
    }
    PublishedService find(UUID id, Query query);
    PublishedService metadata(UUID id, SupportedLanguage language);
}
