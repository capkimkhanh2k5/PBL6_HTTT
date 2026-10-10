package com.danasea.backend.modules.ai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.danasea.backend.modules.ai.application.api.CustomerPreferenceReadApi;
import com.danasea.backend.modules.ai.application.dtos.TravelRequest;
import com.danasea.backend.modules.ai.application.ports.*;
import com.danasea.backend.modules.ai.application.usecases.*;
import com.danasea.backend.modules.ai.domain.models.*;
import com.danasea.backend.modules.ai.domain.services.TravelQueryParser;
import com.danasea.backend.modules.ai.infrastructure.adapters.TravelWeatherAdapter;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.*;
import com.danasea.backend.modules.weather.application.dtos.AdvanceBookingSafetyResponse;
import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.usecases.CheckAdvanceBookingSafetyUseCase;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerDiscoveryUpgradeTest {
    private final LocalDate date = LocalDate.now(TravelContext.ZONE).plusDays(1);
    private TravelContext context(List<String> interests, boolean consent) {
        return new TravelContext("", null, null, 3, date, date, null, null, null, null, null,
                interests, 15, 3, false, new TravelCriteria("TOTAL", null, null, null, null, null, 0, consent));
    }
    private TravelRequest request(String query) {
        return new TravelRequest(query, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    private PublishedService service(String name, String pricing, String unit, int quantity, boolean bookable) {
        Slot slot = new Slot(UUID.randomUUID(), date, LocalTime.of(9, 0), LocalTime.of(10, 0), 10);
        Option option = new Option(UUID.randomUUID(), name, pricing, new BigDecimal(unit), quantity,
                new BigDecimal(unit).multiply(BigDecimal.valueOf(quantity)), "Guide", bookable ? List.of(slot) : List.of());
        return new PublishedService(UUID.randomUUID(), UUID.randomUUID(), name, "Trip", null, "Water", "water", "Da Nang",
                16.1, 108.2, 60, true, new BigDecimal("4.5"), 8, List.of(option));
    }
    @Test void multiActivityNegationAndTimeBudgetHaveExplicitResolvedFields() {
        var resolved = request("3 người ngày mai không SUP, tìm kayak từ 9h đến 16h dưới 200k mỗi người").context();
        assertThat(resolved.partySize()).isEqualTo(3);
        assertThat(resolved.from()).isEqualTo(date);
        assertThat(resolved.dayStart()).isEqualTo(LocalTime.of(9, 0));
        assertThat(resolved.dayEnd()).isEqualTo(LocalTime.of(16, 0));
        assertThat(resolved.totalBudget()).isEqualByComparingTo("600000");
        assertThat(resolved.criteria().budgetBasis()).isEqualTo("PER_PERSON");
        assertThat(resolved.criteria().excludedActivities()).containsExactly("sup");
        assertThat(resolved.criteria().includedActivities()).containsExactly("kayak");
        assertThat(TravelQueryParser.includedActivities("SUP và kayak")).containsExactly("kayak", "sup");
    }
    @Test void familyQueryClarifiesNumberBeforeCatalogOrModel() {
        var data = mock(TravelDataPort.class); var model = mock(DecisionModelPort.class); var weather = mock(TravelWeatherPort.class);
        var result = new DiscoverServicesUseCase(data, model, weather).execute(request("đi cuối tuần cho gia đình dưới 1 triệu").context(), "SEARCH", SupportedLanguage.VI);
        assertThat(result.status()).isEqualTo("NEEDS_INPUT");
        assertThat(result.requiredInputs()).contains("PARTY_SIZE_REQUIRED");
        verifyNoInteractions(data, model, weather);
    }
    @Test void unsupportedChildOrEquipmentConditionsRemainVisible() {
        var context = request("3 người ngày mai kayak cho trẻ em có thiết bị").context();
        assertThat(context.criteria().unsupportedConstraints()).contains("PARTICIPATION_REQUIREMENTS_NEED_VENDOR_VERIFICATION");
        assertThatThrownBy(() -> request("3 người 2026-13-40 lúc 25h").context()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TravelContext("", null, null, 1, date, date, null, null, null, null, null, List.of(), 16, 3, false)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void consentedSignalsChangeRankingAndBindRecommendationToReturnedServices() {
        var data = mock(TravelDataPort.class); var model = mock(DecisionModelPort.class); var weather = mock(TravelWeatherPort.class);
        var prefs = mock(CustomerPreferenceReadApi.class); var useCase = new DiscoverServicesUseCase(data, model, weather); useCase.setPreferences(prefs);
        UUID owner = UUID.randomUUID(), resultId = UUID.randomUUID();
        var kayak = service("Kayak", "PER_PERSON", "100000", 3, true); var sup = service("SUP", "PER_PERSON", "100000", 3, true);
        when(data.search(any())).thenReturn(List.of(sup, kayak));
        when(model.decide(any(), any())).thenAnswer(call -> DecisionResult.unavailable(call.getArgument(0), "OFFLINE"));
        when(prefs.load(owner)).thenReturn(new CustomerPreferenceReadApi.Signals(true, List.of("kayak"), List.of(), Set.of(), Set.of(), Set.of(), Set.of()));
        when(prefs.recordRecommendation(eq(owner), any(), any())).thenReturn(resultId);
        var result = useCase.execute(context(List.of(), true), "RECOMMENDATION", SupportedLanguage.VI, owner);
        assertThat(result.candidates().getFirst().service().id()).isEqualTo(kayak.id());
        assertThat(result.recommendationId()).isEqualTo(resultId);
        verify(prefs).recordRecommendation(eq(owner), eq(result.candidates().stream().map(candidate -> candidate.service().id()).toList()), matches("[a-f0-9]{64}"));
        clearInvocations(prefs);
        useCase.execute(context(List.of(), false), "RECOMMENDATION", SupportedLanguage.VI, owner);
        verify(prefs, never()).load(any());
    }
    @Test void comparisonUsesSamePartyTotalAndSeparatesUnavailableOption() {
        var data = mock(TravelDataPort.class); var discovery = new DiscoverServicesUseCase(data, mock(DecisionModelPort.class), mock(TravelWeatherPort.class));
        var packageService = service("Private", "PER_PACKAGE", "150000", 2, true);
        var perPerson = service("Group", "PER_PERSON", "110000", 3, true);
        var unavailable = service("Full", "PER_PERSON", "1000", 3, false);
        when(data.find(eq(packageService.id()), any())).thenReturn(packageService);
        when(data.find(eq(perPerson.id()), any())).thenReturn(perPerson);
        when(data.find(eq(unavailable.id()), any())).thenReturn(unavailable);
        var result = new CompareServicesUseCase(discovery, mock(TravelPolicyPort.class)).execute(List.of(packageService.id(), perPerson.id(), unavailable.id()), context(List.of(), false), SupportedLanguage.VI);
        assertThat(result.matrix().getFirst().partyTotal()).isEqualByComparingTo("300000");
        assertThat(result.matrix().get(1).partyTotal()).isEqualByComparingTo("330000");
        assertThat(result.matrix().get(2).availability()).isEqualTo("UNAVAILABLE");
        assertThat(result.bestFit().serviceId()).isEqualTo(packageService.id());
        assertThat(result.bestFit().bookingAuthorized()).isFalse();
    }
    @Test void missingOrExpiredForecastCannotClaimAcceptable() {
        var safety = mock(CheckAdvanceBookingSafetyUseCase.class); var adapter = new TravelWeatherAdapter(safety);
        var service = service("Kayak", "PER_PERSON", "100000", 3, true); var slot = service.options().getFirst().slots().getFirst();
        var forecast = WeatherInfoDto.TimeWindowForecast.builder().sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(1800)).peakWindSpeed(5.0).provider("OPEN_METEO_WEATHER_AND_MARINE")
                .sourceFetchedAt(Instant.now().minusSeconds(8000)).validUntil(Instant.now().minusSeconds(1)).build();
        when(safety.checkBySlotId(slot.id())).thenReturn(AdvanceBookingSafetyResponse.builder().isSafe(true).forecast(forecast).build());
        var expired = adapter.assess(service, slot);
        assertThat(expired.status()).isEqualTo("STALE");
        assertThat(expired.acceptable()).isFalse();
        assertThat(expired.requiresRevalidation()).isTrue();
        forecast.setSourceFetchedAt(null);
        assertThat(adapter.assess(service, slot).acceptable()).isFalse();
        forecast.setSourceFetchedAt(Instant.now()); forecast.setValidUntil(Instant.now().plusSeconds(60));
        assertThat(adapter.assess(service, slot).acceptable()).isTrue();
    }
}
