package com.danasea.backend.modules.ai.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.ai.application.port.ItineraryStorePort;
import com.danasea.backend.modules.ai.application.port.TravelWeatherPort;
import com.danasea.backend.modules.ai.domain.exceptions.AiResourceNotFoundException;
import com.danasea.backend.modules.ai.domain.exceptions.AiStateConflictException;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Item;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Saved;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan;
import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.modules.ai.domain.services.TravelQueryParser;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Option;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.PublishedService;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Slot;
import com.danasea.backend.shared.i18n.SupportedLanguage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanItineraryUseCase {
    public record Replanned(Saved itinerary, List<UUID> removedSlotIds, List<UUID> addedSlotIds,
                            List<Item> changedItems, String trigger, boolean bookingChanged) {}
    private record Choice(PublishedService service, Option option, Slot slot) {}

    private final DiscoverServicesUseCase discovery;
    private final TravelWeatherPort weather;
    private final ItineraryStorePort store;

    public Saved create(UUID ownerId, TravelContext context, SupportedLanguage language) {
        requireOwner(ownerId);
        return store.create(ownerId, propose(context, Set.of(), Set.of(), language));
    }

    public Saved get(UUID id, UUID ownerId) {
        requireOwner(ownerId);
        return store.find(id, ownerId).orElseThrow(() -> new AiResourceNotFoundException("Itinerary not found"));
    }

    public List<Saved> list(UUID ownerId) { requireOwner(ownerId); return store.list(ownerId); }

    public Replanned replan(UUID id, UUID ownerId, long expectedVersion, TravelContext replacement,
                            Set<UUID> excludedServices, Set<UUID> excludedSlots, String trigger, SupportedLanguage language) {
        Saved old = get(id, ownerId);
        if (old.version() != expectedVersion) throw new AiStateConflictException("Itinerary version has changed");
        if (excludedServices == null || excludedSlots == null || excludedServices.stream().anyMatch(Objects::isNull) || excludedSlots.stream().anyMatch(Objects::isNull) || excludedServices.size() > 20 || excludedSlots.size() > 50 || trigger == null || trigger.length() > 100) {
            throw new IllegalArgumentException("Invalid replanning exclusions or trigger");
        }
        ItineraryPlan next = propose(replacement == null ? old.plan().context() : replacement, excludedServices, excludedSlots, language);
        Saved saved = store.replace(id, ownerId, expectedVersion, next);
        Set<UUID> oldIds = old.plan().items().stream().map(Item::slotId).collect(Collectors.toSet());
        Set<UUID> newIds = next.items().stream().map(Item::slotId).collect(Collectors.toSet());
        return new Replanned(saved, oldIds.stream().filter(slot -> !newIds.contains(slot)).sorted().toList(),
                newIds.stream().filter(slot -> !oldIds.contains(slot)).sorted().toList(),
                next.items().stream().filter(item -> old.plan().items().stream().anyMatch(previous -> previous.slotId().equals(item.slotId()) && !previous.equals(item))).toList(), trigger, false);
    }

    public ItineraryPlan propose(TravelContext context, Set<UUID> excludedServices, Set<UUID> excludedSlots, SupportedLanguage language) {
        var found = discovery.execute(context, "RECOMMENDATION", language);
        List<Choice> choices = found.candidates().stream().limit(10)
                .filter(candidate -> !excludedServices.contains(candidate.service().id()))
                .flatMap(candidate -> candidate.service().options().stream().flatMap(option -> option.slots().stream().limit(5)
                        .filter(slot -> !excludedSlots.contains(slot.id())).map(slot -> new Choice(candidate.service(), option, slot))))
                .sorted(Comparator.comparing((Choice choice) -> LocalDateTime.of(choice.slot().date(), choice.slot().start()))
                        .thenComparing(choice -> choice.option().partyTotal()).thenComparing(choice -> choice.service().id()))
                .toList();
        List<Item> items = new ArrayList<>();
        Set<UUID> usedServices = new LinkedHashSet<>();
        Set<String> unresolved = new LinkedHashSet<>();
        Map<UUID, TravelWeatherPort.Assessment> weatherCache = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Choice choice : choices) {
            if (items.size() >= context.maxActivities()) break;
            if (usedServices.contains(choice.service().id())) continue;
            BigDecimal nextTotal = total.add(choice.option().partyTotal());
            if (found.effectiveTotalBudget() != null && nextTotal.compareTo(found.effectiveTotalBudget()) > 0) continue;
            LocalDateTime start = LocalDateTime.of(choice.slot().date(), choice.slot().start());
            LocalDateTime end = LocalDateTime.of(choice.slot().date(), DiscoverServicesUseCase.endTime(choice.slot().end(), choice.slot().start(), choice.service().durationMinutes()));
            int transit = 0;
            if (!items.isEmpty()) {
                Item previous = items.get(items.size() - 1);
                if (previous.latitude() == null || previous.longitude() == null
                        || choice.service().latitude() == null || choice.service().longitude() == null) {
                    unresolved.add("MISSING_TRANSFER_COORDINATES");
                    continue;
                }
                double km = TravelQueryParser.distanceKm(previous.latitude(), previous.longitude(), choice.service().latitude(), choice.service().longitude());
                transit = 15 + (int) Math.ceil(km / 25 * 60);
                if (start.isBefore(previous.end().plusMinutes(transit))) continue;
            }
            var assessment = weatherCache.computeIfAbsent(choice.slot().id(), ignored -> weather.assess(choice.service(), choice.slot()));
            if (!assessment.acceptable()) { unresolved.add("WEATHER_" + assessment.status()); continue; }
            items.add(new Item(choice.service().id(), choice.option().id(), choice.slot().id(), choice.service().name(),
                    start, end, choice.option().quantity(), choice.option().unitPrice(), choice.option().partyTotal(),
                    choice.service().latitude(), choice.service().longitude(), transit, assessment.status(),
                    assessment.provisional(), assessment.message()));
            usedServices.add(choice.service().id());
            total = nextTotal;
        }
        if (items.isEmpty()) unresolved.add("NO_FEASIBLE_CURRENT_SLOTS");
        String status = items.isEmpty() ? "NEEDS_REVIEW" : items.stream().anyMatch(Item::provisionalWeather) ? "PROVISIONAL" : "PROPOSED";
        return new ItineraryPlan(status, context, total, List.copyOf(items), List.copyOf(unresolved),
                "BOUNDED_CHRONOLOGICAL_GREEDY_WITH_ESTIMATED_TRANSFERS", false);
    }

    private void requireOwner(UUID ownerId) {
        if (ownerId == null) throw new IllegalArgumentException("An authenticated owner is required");
    }
}
