package com.danasea.backend.modules.ai.application.usecase;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.ai.application.port.DecisionModelPort;
import com.danasea.backend.modules.ai.application.port.TravelDataPort;
import com.danasea.backend.modules.ai.application.port.TravelWeatherPort;
import com.danasea.backend.modules.ai.domain.models.DecisionResult;
import com.danasea.backend.modules.ai.domain.models.DecisionTask;
import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.modules.ai.domain.services.TravelQueryParser;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Option;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.PublishedService;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Query;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Slot;
import com.danasea.backend.shared.i18n.SupportedLanguage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DiscoverServicesUseCase {
    public record Candidate(PublishedService service, BigDecimal minimumPartyTotal, Double distanceKm,
                            double score, List<String> reasonCodes, DecisionResult relevance) {}
    public record Result(String status, String mode, TravelContext context, String retrievalKeyword,
                         BigDecimal effectiveTotalBudget, List<Candidate> candidates, List<String> requiredInputs,
                         DecisionResult intent, String rankingMethod, boolean exhaustive, List<String> limitations) {}

    private final TravelDataPort data;
    private final DecisionModelPort decisions;
    private final TravelWeatherPort weather;

    public Result execute(TravelContext context, String mode, SupportedLanguage language) {
        context.validateCurrentDates();
        if (!List.of("SEARCH", "RECOMMENDATION", "NEARBY").contains(mode)) throw new IllegalArgumentException("Invalid discovery mode");
        if ("NEARBY".equals(mode) && context.latitude() == null) {
            return new Result("NEEDS_INPUT", mode, context, null, context.totalBudget(), List.of(),
                    List.of("latitude", "longitude"), DecisionResult.unavailable(DecisionTask.INTENT, "NOT_REQUESTED"), "CONTEXTUAL_BASELINE", false, List.of("CATALOG_SAMPLE_LIMIT_50"));
        }
        String keyword = TravelQueryParser.activityKeyword(context.query());
        BigDecimal budget = context.totalBudget() != null ? context.totalBudget() : TravelQueryParser.budget(context.query());
        if (context.totalBudget() == null && budget != null && TravelQueryParser.perPersonBudget(context.query())) {
            budget = budget.multiply(BigDecimal.valueOf(context.partySize()));
        }
        final BigDecimal effectiveBudget = budget;
        if (budget != null && budget.compareTo(new BigDecimal("1000000000")) > 0) {
            throw new IllegalArgumentException("Parsed budget exceeds the supported maximum");
        }
        Query query = query(context, keyword, language);
        List<Candidate> candidates = new ArrayList<>();
        Map<UUID, TravelWeatherPort.Assessment> weatherCache = new HashMap<>();
        List<String> exclusions = TravelQueryParser.excludedActivities(context.query());
        for (PublishedService service : data.search(query)) {
            String categoryText = TravelQueryParser.normalize(service.name() + " " + service.categoryName() + " " + service.categorySlug());
            if (exclusions.stream().anyMatch(categoryText::contains)) continue;
            List<Option> validOptions = service.options().stream()
                    .filter(option -> effectiveBudget == null || option.partyTotal().compareTo(effectiveBudget) <= 0)
                    .map(option -> new Option(option.id(), option.name(), option.pricingUnit(), option.unitPrice(),
                            option.quantity(), option.partyTotal(), option.benefits(), option.slots().stream()
                            .filter(slot -> slot.start() != null && !slot.start().isBefore(context.dayStart()))
                            .filter(slot -> endTime(slot.end(), slot.start(), service.durationMinutes()) != null)
                            .filter(slot -> !endTime(slot.end(), slot.start(), service.durationMinutes()).isAfter(context.dayEnd()))
                            .filter(slot -> !context.weatherSafeOnly() || weatherAcceptable(service, slot, weatherCache)).toList()))
                    .filter(option -> !option.slots().isEmpty()).toList();
            if (validOptions.isEmpty()) continue;
            Double distance = distance(context, service);
            if ("NEARBY".equals(mode) && distance == null || context.radiusKm() != null && (distance == null || distance > context.radiusKm())) continue;
            double baseline = baseline(context, service, distance);
            DecisionResult relevance = DecisionResult.unavailable(DecisionTask.RELEVANCE, "NOT_REQUESTED");
            if ("RECOMMENDATION".equals(mode) && candidates.size() < 10) {
                relevance = decisions.decide(DecisionTask.RELEVANCE,
                        Map.of("context", Map.of("query", context.query(), "interests", context.interests()),
                                "service", Map.of("name", service.name(), "description", truncate(service.description(), 900))));
                Double score = relevance.score("relevance");
                if (score != null) baseline += score / 3.0;
            }
            PublishedService snapshot = new PublishedService(service.id(), service.vendorId(), service.name(), service.description(),
                    service.categoryId(), service.categoryName(), service.categorySlug(), service.address(), service.latitude(),
                    service.longitude(), service.durationMinutes(), service.weatherSensitive(), service.averageRating(), service.reviewCount(), validOptions);
            List<String> reasons = new ArrayList<>(List.of("PUBLISHED", "CURRENT_BOOKABLE_OPTION", "PARTY_QUOTE"));
            if (budget != null) reasons.add("WITHIN_TOTAL_BUDGET");
            if (distance != null) reasons.add("DISTANCE_KNOWN");
            if (relevance.available()) reasons.add("QUYET_RELEVANCE_SUGGESTION");
            candidates.add(new Candidate(snapshot, validOptions.stream().map(Option::partyTotal).min(BigDecimal::compareTo).orElseThrow(),
                    distance, baseline, List.copyOf(reasons), relevance));
        }
        Comparator<Candidate> comparator = "NEARBY".equals(mode)
                ? Comparator.comparing(Candidate::distanceKm, Comparator.nullsLast(Double::compareTo)).thenComparing(Candidate::minimumPartyTotal)
                : Comparator.comparingDouble(Candidate::score).reversed().thenComparing(candidate -> candidate.service().id());
        List<Candidate> ranked = candidates.stream().sorted(comparator).limit(context.limit()).toList();
        DecisionResult intent = context.query().isBlank() ? DecisionResult.unavailable(DecisionTask.INTENT, "EMPTY_QUERY")
                : decisions.decide(DecisionTask.INTENT, Map.of("message", context.query()));
        return new Result(ranked.isEmpty() ? "NO_MATCHES" : "AVAILABLE", mode, context, keyword, budget, ranked,
                List.of(), intent, "RECOMMENDATION".equals(mode) ? "CONTEXTUAL_BASELINE_WITH_OPTIONAL_QUYET" : "CATALOG_BASELINE", false, context.weatherSafeOnly()
                ? List.of("CATALOG_SAMPLE_LIMIT_50", "WEATHER_ASSESSMENT_LIMIT_20") : List.of("CATALOG_SAMPLE_LIMIT_50"));
    }

    private boolean weatherAcceptable(PublishedService service, Slot slot,
                                      Map<UUID, TravelWeatherPort.Assessment> cache) {
        if (!service.weatherSensitive()) return true;
        if (!cache.containsKey(slot.id()) && cache.size() >= 20) return false;
        return cache.computeIfAbsent(slot.id(), ignored -> weather.assess(service, slot)).acceptable();
    }

    public PublishedService detail(UUID id, TravelContext context, SupportedLanguage language) {
        return data.find(id, query(context, null, language));
    }

    public Query query(TravelContext context, String keyword, SupportedLanguage language) {
        return new Query(keyword, context.categoryId(), context.from(), context.to(), context.partySize(),
                context.latitude(), context.longitude(), context.radiusKm(), 50, language);
    }

    private double baseline(TravelContext context, PublishedService service, Double distance) {
        double score = service.averageRating() == null ? 0 : service.averageRating().doubleValue() / 5.0;
        String text = TravelQueryParser.normalize(service.name() + " " + service.description() + " " + service.categoryName());
        for (String interest : context.interests()) if (text.contains(TravelQueryParser.normalize(interest))) score += 1;
        if (distance != null) score += 1 / (1 + distance);
        return score;
    }

    private Double distance(TravelContext context, PublishedService service) {
        if (context.latitude() == null || service.latitude() == null || service.longitude() == null) return null;
        return TravelQueryParser.distanceKm(context.latitude(), context.longitude(), service.latitude(), service.longitude());
    }

    public static LocalTime endTime(LocalTime end, LocalTime start, Integer duration) {
        if (end != null && start != null && end.isAfter(start)) return end;
        if (end == null && start != null && duration != null && duration > 0 && duration < 1440) {
            LocalTime derived = start.plusMinutes(duration);
            return derived.isAfter(start) ? derived : null;
        }
        return null;
    }

    private String truncate(String text, int limit) { return text == null ? "" : text.substring(0, Math.min(limit, text.length())); }
}
