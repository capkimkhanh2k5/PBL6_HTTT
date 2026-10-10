package com.danasea.backend.modules.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.danasea.backend.modules.ai.application.port.DecisionModelPort;
import com.danasea.backend.modules.ai.application.port.ItineraryStorePort;
import com.danasea.backend.modules.ai.application.port.TravelDataPort;
import com.danasea.backend.modules.ai.application.port.TravelWeatherPort;
import com.danasea.backend.modules.ai.application.usecase.DiscoverServicesUseCase;
import com.danasea.backend.modules.ai.application.usecase.PlanItineraryUseCase;
import com.danasea.backend.modules.ai.domain.exceptions.AiStateConflictException;
import com.danasea.backend.modules.ai.domain.models.DecisionResult;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Proposal;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Saved;
import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Option;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.PublishedService;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Slot;
import com.danasea.backend.shared.i18n.SupportedLanguage;

class ItineraryPlannerUpgradeTest {
    private final TravelDataPort data = mock(TravelDataPort.class);
    private final DecisionModelPort decisions = mock(DecisionModelPort.class);
    private final TravelWeatherPort weather = mock(TravelWeatherPort.class);
    private final ItineraryStorePort store = mock(ItineraryStorePort.class);
    private final LocalDate tomorrow = LocalDate.now(TravelContext.ZONE).plusDays(1);
    private final UUID owner = UUID.randomUUID();
    private PlanItineraryUseCase planner;

    @BeforeEach void setup() {
        planner = new PlanItineraryUseCase(new DiscoverServicesUseCase(data, decisions, weather), weather, store);
        when(decisions.decide(any(), any())).thenAnswer(call -> DecisionResult.unavailable(call.getArgument(0), "TEST_OFFLINE"));
        when(weather.assess(any(), any())).thenReturn(new TravelWeatherPort.Assessment("NOT_REQUIRED", true, false, null, null, null));
    }

    private TravelContext context(LocalDate end, int activities, BigDecimal budget) {
        return new TravelContext("", null, budget, 1, tomorrow, end, LocalTime.of(8,0), LocalTime.of(18,0),
                null, null, null, List.of(), 15, activities, false);
    }

    private PublishedService service(String name, LocalDate date, int start, int end, String price) {
        Slot slot = new Slot(UUID.randomUUID(), date, LocalTime.of(start,0), LocalTime.of(end,0), 10);
        Option option = new Option(UUID.randomUUID(), name, "PER_PERSON", new BigDecimal(price), 1,
                new BigDecimal(price), "", List.of(slot));
        return new PublishedService(UUID.randomUUID(), UUID.randomUUID(), name, "Coastal trip", null, "Water", "water",
                "Da Nang", 16.1, 108.2, 60, false, null, 0, List.of(option));
    }

    @Test void activityLimitAppliesToEachDayAndTransfersDoNotCarryAcrossDays() {
        var first = service("First", tomorrow, 9, 10, "100");
        var sameDay = service("Second", tomorrow, 12, 13, "100");
        var nextDay = service("Third", tomorrow.plusDays(1), 9, 10, "100");
        when(data.search(any())).thenReturn(List.of(first, sameDay, nextDay));
        var plan = planner.propose(context(tomorrow.plusDays(1), 1, null), Set.of(), Set.of(), SupportedLanguage.VI);
        assertThat(plan.items()).hasSize(2);
        assertThat(plan.items()).extracting(item -> item.start().toLocalDate()).containsExactly(tomorrow, tomorrow.plusDays(1));
        assertThat(plan.items().get(1).estimatedTravelMinutes()).isZero();
        assertThat(plan.totalPrice()).isEqualByComparingTo("200");
    }

    @Test void alternativeSearchCanSkipCheapEarlySlotThatBlocksTwoLaterActivities() {
        var blocking = service("Long cheap", tomorrow, 9, 12, "50");
        var first = service("Short first", tomorrow, 9, 10, "60");
        var second = service("Short second", tomorrow, 11, 12, "60");
        when(data.search(any())).thenReturn(List.of(blocking, first, second));
        var alternatives = planner.alternatives(context(tomorrow, 3, new BigDecimal("200")), Set.of(), Set.of(), List.of(), SupportedLanguage.VI);
        assertThat(alternatives).hasSizeBetween(2, 3);
        assertThat(alternatives.getFirst().plan().items()).extracting(ItineraryPlan.Item::serviceId).containsExactly(first.id(), second.id());
        assertThat(alternatives.stream().map(value -> value.plan().items()).distinct()).hasSameSizeAs(alternatives);
        assertThat(alternatives.getFirst().plan().unresolvedConstraints()).contains("MEALS_AND_TRANSFER_COSTS_NOT_INCLUDED");
        assertThat(alternatives.getFirst().plan().inventoryReserved()).isFalse();
    }

    @Test void weatherBeyondHorizonCanBeTentativeButCannotBeAccepted() {
        var service = service("Future kayak", tomorrow, 9, 10, "100");
        when(data.search(any())).thenReturn(List.of(service));
        when(data.find(eq(service.id()), any())).thenReturn(service);
        when(weather.assess(any(), any())).thenReturn(new TravelWeatherPort.Assessment("WAITING_FOR_FORECAST", false, true, null, null, null));
        var plan = planner.propose(context(tomorrow, 3, null), Set.of(), Set.of(), SupportedLanguage.VI);
        assertThat(plan.status()).isEqualTo("WAITING_FOR_FORECAST");
        assertThat(plan.items().getFirst().provisionalWeather()).isTrue();
        var saved = new Saved(UUID.randomUUID(), owner, 0, plan, OffsetDateTime.now(), OffsetDateTime.now());
        when(store.find(saved.id(), owner)).thenReturn(Optional.of(saved));
        assertThatThrownBy(() -> planner.accept(saved.id(), owner, 0, SupportedLanguage.VI)).isInstanceOf(AiStateConflictException.class);
        verify(store, never()).transition(any(), any(), anyLong(), any(), any());
    }

    @Test void replanReturnsProposalAndCannotForgeWeatherEventOrOverwriteCurrentSchedule() {
        var service = service("Old kayak", tomorrow, 9, 10, "100");
        when(data.search(any())).thenReturn(List.of(service));
        ItineraryPlan plan = planner.propose(context(tomorrow, 3, null), Set.of(), Set.of(), SupportedLanguage.VI);
        Saved saved = new Saved(UUID.randomUUID(), owner, 4, plan, OffsetDateTime.now(), OffsetDateTime.now(), "ACCEPTED", false);
        when(store.find(saved.id(), owner)).thenReturn(Optional.of(saved));
        when(store.propose(eq(saved.id()), eq(owner), eq(4L), any(), eq("CUSTOMER_REQUEST"), eq(null)))
                .thenAnswer(call -> new Proposal(UUID.randomUUID(), saved.id(), 4, "PENDING", call.getArgument(3),
                        List.of(), List.of(), List.of(), "CUSTOMER_REQUEST", null, OffsetDateTime.now(), false));
        var result = planner.replan(saved.id(), owner, 4, null, Set.of(), Set.of(), "WEATHER_RED", SupportedLanguage.VI);
        assertThat(result.itinerary()).isEqualTo(saved);
        assertThat(result.proposal().state()).isEqualTo("PENDING");
        assertThat(result.trigger()).isEqualTo("CUSTOMER_REQUEST");
        assertThat(result.bookingChanged()).isFalse();
        verify(store, never()).replace(any(), any(), anyLong(), any());
    }

    @Test void authenticSourceEventPreservesUnaffectedItemsAndDeduplicates() {
        var impacted = service("Impacted", tomorrow, 9, 10, "100");
        var unaffected = service("Unaffected", tomorrow, 12, 13, "100");
        when(data.search(any())).thenReturn(List.of(impacted, unaffected));
        ItineraryPlan plan = planner.propose(context(tomorrow, 3, null), Set.of(), Set.of(), SupportedLanguage.VI);
        Saved saved = new Saved(UUID.randomUUID(), owner, 2, plan, OffsetDateTime.now(), OffsetDateTime.now(), "ACCEPTED", false);
        Saved stale = new Saved(saved.id(), owner, 3, plan, saved.createdAt(), saved.updatedAt(), "STALE", false);
        UUID impactedSlot = impacted.options().getFirst().slots().getFirst().id();
        when(store.trackedForSlot(impactedSlot)).thenReturn(List.of(saved));
        when(store.transition(saved.id(), owner, 2, "STALE", "CANONICAL_SLOT_UNAVAILABLE")).thenReturn(stale);
        when(data.search(any())).thenReturn(List.of());
        planner.sourceChanged(impactedSlot, "SLOT:canonical:1", "CANONICAL_SLOT_UNAVAILABLE");
        verify(store).propose(eq(saved.id()), eq(owner), eq(3L), org.mockito.ArgumentMatchers.argThat(next ->
                next.items().equals(plan.items().stream().filter(item -> !item.slotId().equals(impactedSlot)).toList())),
                eq("CANONICAL_SLOT_UNAVAILABLE"), eq("SLOT:canonical:1"));
        when(store.hasSourceEvent(saved.id(), "SLOT:canonical:1")).thenReturn(true);
        planner.sourceChanged(impactedSlot, "SLOT:canonical:1", "CANONICAL_SLOT_UNAVAILABLE");
        verify(store, org.mockito.Mockito.times(1)).transition(any(), any(), anyLong(), any(), any());
    }

    @Test void ongoingItineraryPreservesPastItemsAndOnlySearchesRemainingDates() {
        var futureService = service("Remaining", tomorrow, 9, 10, "100");
        var current = context(tomorrow, 3, null);
        var pastContext = new TravelContext("", null, null, 1, tomorrow.minusDays(2), tomorrow, LocalTime.of(8,0), LocalTime.of(18,0),
                null, null, null, List.of(), 15, 3, false);
        var past = new ItineraryPlan.Item(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Completed",
                tomorrow.minusDays(2).atTime(9,0), tomorrow.minusDays(2).atTime(10,0), 1, new BigDecimal("100"), new BigDecimal("100"),
                16.1, 108.2, 0, "NOT_REQUIRED", false, null);
        when(data.search(any())).thenReturn(List.of(futureService));
        var futurePlan = planner.propose(current, Set.of(), Set.of(), SupportedLanguage.VI);
        var items = new java.util.ArrayList<>(futurePlan.items());
        items.addFirst(past);
        var original = new ItineraryPlan("PROPOSED", pastContext, new BigDecimal("200"), List.copyOf(items), List.of(), "TEST", false);
        var saved = new Saved(UUID.randomUUID(), owner, 0, original, OffsetDateTime.now(), OffsetDateTime.now(), "ACCEPTED", false);
        when(store.find(saved.id(), owner)).thenReturn(Optional.of(saved));
        when(store.propose(eq(saved.id()), eq(owner), eq(0L), any(), any(), eq(null))).thenAnswer(call ->
                new Proposal(UUID.randomUUID(), saved.id(), 0, "PENDING", call.getArgument(3), List.of(), List.of(), List.of(), "CUSTOMER_REQUEST", null, OffsetDateTime.now(), false));
        var result = planner.replan(saved.id(), owner, 0, null, Set.of(), Set.of(), "CHANGE", SupportedLanguage.VI);
        assertThat(result.proposal().plan().items()).contains(past);
        assertThat(result.proposal().plan().context()).isEqualTo(pastContext);
        verify(data).search(org.mockito.ArgumentMatchers.argThat(query -> query.from().equals(LocalDate.now(TravelContext.ZONE))));
    }

    @Test void acceptedQuoteRejectsChangedSlotEndOrCoordinates() {
        var service = service("Kayak", tomorrow, 9, 10, "100");
        when(data.search(any())).thenReturn(List.of(service));
        var original = planner.propose(context(tomorrow, 3, null), Set.of(), Set.of(), SupportedLanguage.VI);
        var saved = new Saved(UUID.randomUUID(), owner, 0, original, OffsetDateTime.now(), OffsetDateTime.now());
        when(store.find(saved.id(), owner)).thenReturn(Optional.of(saved));
        var option = service.options().getFirst();
        var slot = option.slots().getFirst();
        var changedSlot = new Slot(slot.id(), slot.date(), slot.start(), LocalTime.of(11,0), 10);
        var changedOption = new Option(option.id(), option.name(), option.pricingUnit(), option.unitPrice(), option.quantity(), option.partyTotal(), option.benefits(), List.of(changedSlot));
        when(data.find(eq(service.id()), any())).thenReturn(new PublishedService(service.id(), service.vendorId(), service.name(), service.description(),
                service.categoryId(), service.categoryName(), service.categorySlug(), service.address(), service.latitude(), service.longitude(),
                service.durationMinutes(), service.weatherSensitive(), service.averageRating(), service.reviewCount(), List.of(changedOption)));
        assertThatThrownBy(() -> planner.accept(saved.id(), owner, 0, SupportedLanguage.VI)).isInstanceOf(AiStateConflictException.class);
        verify(store, never()).transition(any(), any(), anyLong(), any(), any());
    }
}
