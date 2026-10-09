package com.danasea.backend.modules.ai.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.ai.application.port.TravelPolicyPort;
import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.modules.ai.domain.services.TravelQueryParser;
import com.danasea.backend.modules.order.application.api.AiPolicyReadApi.Snapshot;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Option;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.PublishedService;
import com.danasea.backend.shared.i18n.SupportedLanguage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompareServicesUseCase {
    public record Row(UUID serviceId, String serviceName, String availability, UUID selectedOptionId,
                      String pricingUnit, Integer quantity, BigDecimal unitPrice, BigDecimal partyTotal,
                      Integer durationMinutes, BigDecimal rating, int reviewCount, Double distanceKm,
                      String benefits, int bookableSlots, List<String> advantages, List<String> limitations) {}
    public record Difference(String field, Map<UUID, Object> values, String source) {}
    public record BestFit(UUID serviceId, UUID optionId, List<String> reasons, String method, boolean bookingAuthorized) {}
    public record Result(List<PublishedService> services, List<String> comparedFields,
                         String priceBasis, Snapshot commonPolicy, boolean inventoryReserved, List<Row> matrix,
                         List<Difference> keyDifferences, BestFit bestFit, TravelContext resolvedCriteria,
                         Instant checkedAt, String quoteValidity, List<String> limitations) {}
    private final DiscoverServicesUseCase discovery;
    private final TravelPolicyPort policies;

    public Result execute(List<UUID> serviceIds, TravelContext context, SupportedLanguage language) {
        context.validateCurrentDates();
        if (serviceIds == null || serviceIds.size() < 2 || serviceIds.size() > 4
                || serviceIds.stream().anyMatch(id -> id == null) || serviceIds.stream().distinct().count() != serviceIds.size()) {
            throw new IllegalArgumentException("Provide two to four distinct service IDs");
        }
        List<PublishedService> services = serviceIds.stream().map(id -> discovery.detail(id, context, language)).toList();
        List<Row> rows = new ArrayList<>();
        BigDecimal cheapest = services.stream().flatMap(service -> service.options().stream()).filter(option -> !option.slots().isEmpty())
                .map(Option::partyTotal).min(BigDecimal::compareTo).orElse(null);
        for (var service : services) {
            Option option = service.options().stream().filter(candidate -> !candidate.slots().isEmpty())
                    .min(Comparator.comparing(Option::partyTotal).thenComparing(Option::id)).orElse(null);
            List<String> advantages = new ArrayList<>();
            List<String> limits = new ArrayList<>(List.of("VENDOR_PARTICIPATION_REQUIREMENTS_NOT_VERIFIED"));
            if (option == null) limits.add("NO_CURRENT_BOOKABLE_OPTION_FOR_CRITERIA");
            if (option != null && option.partyTotal().compareTo(cheapest) == 0) advantages.add("LOWEST_CURRENT_PARTY_QUOTE_IN_COMPARISON");
            if (service.reviewCount() < 5) limits.add("LIMITED_REVIEW_EVIDENCE");
            if (service.durationMinutes() == null) limits.add("DURATION_UNAVAILABLE");
            if (service.weatherSensitive()) limits.add("SLOT_WEATHER_REVALIDATION_REQUIRED");
            Double distance = context.latitude() == null || service.latitude() == null || service.longitude() == null ? null
                    : TravelQueryParser.distanceKm(context.latitude(), context.longitude(), service.latitude(), service.longitude());
            rows.add(new Row(service.id(), service.name(), option == null ? "UNAVAILABLE" : "AVAILABLE", option == null ? null : option.id(),
                    option == null ? null : option.pricingUnit(), option == null ? null : option.quantity(), option == null ? null : option.unitPrice(),
                    option == null ? null : option.partyTotal(), service.durationMinutes(), service.averageRating(), service.reviewCount(), distance,
                    option == null ? null : option.benefits(), option == null ? 0 : option.slots().size(), List.copyOf(advantages), List.copyOf(limits)));
        }
        Row fit = rows.stream().filter(row -> row.selectedOptionId() != null)
                .min(Comparator.comparing(Row::partyTotal).thenComparing(Row::distanceKm, Comparator.nullsLast(Double::compareTo)).thenComparing(Row::serviceId)).orElse(null);
        List<Difference> differences = new ArrayList<>();
        for (String field : List.of("partyTotal", "durationMinutes", "distanceKm", "bookableSlots", "reviewCount", "pricingUnit", "benefits")) {
            Map<UUID, Object> values = new LinkedHashMap<>();
            for (Row row : rows) {
                Object value = switch (field) {
                    case "partyTotal" -> row.partyTotal(); case "durationMinutes" -> row.durationMinutes();
                    case "distanceKm" -> row.distanceKm(); case "bookableSlots" -> row.bookableSlots();
                    case "reviewCount" -> row.reviewCount(); case "pricingUnit" -> row.pricingUnit(); default -> row.benefits();
                };
                values.put(row.serviceId(), value == null ? "UNKNOWN" : value);
            }
            if (values.values().stream().distinct().count() > 1) differences.add(new Difference(field, Map.copyOf(values), "CURRENT_CATALOG_OPTION_AND_LIVE_INVENTORY"));
        }
        BestFit best = fit == null ? null : new BestFit(fit.serviceId(), fit.selectedOptionId(),
                List.of("CURRENT_BOOKABLE_PARTY_QUOTE", "LOWEST_PARTY_TOTAL_THEN_DISTANCE"), "DETERMINISTIC_PRICE_DISTANCE_FIT", false);
        return new Result(services, List.of("ACTIVE_OPTION_PRICE", "PARTY_TOTAL", "BENEFITS", "DURATION", "RATING", "REVIEW_COUNT", "CURRENT_SLOTS", "LOCATION", "COMMON_PLATFORM_POLICY"),
                "CURRENT_ACTIVE_OPTION_QUOTE_FOR_PARTY_IN_VND", policies.current(), false, List.copyOf(rows), List.copyOf(differences), best,
                context, Instant.now(), "REVALIDATE_ON_EXPLICIT_BOOKING_CONFIRMATION", List.of("COMMON_POLICY_IS_PLATFORM_WIDE", "RATING_DOES_NOT_PROVE_HIGHEST_QUALITY"));
    }
}
