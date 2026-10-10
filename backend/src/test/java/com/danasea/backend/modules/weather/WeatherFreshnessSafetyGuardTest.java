package com.danasea.backend.modules.weather;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.danasea.backend.modules.communication.application.usecases.SendNotificationUseCase;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.*;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.*;
import com.danasea.backend.modules.weather.application.dtos.*;
import com.danasea.backend.modules.weather.application.ports.output.WeatherProviderPort;
import com.danasea.backend.modules.weather.application.usecases.CheckAdvanceBookingSafetyUseCase;
import com.danasea.backend.modules.weather.domain.models.CategorySafetyRule;
import com.danasea.backend.modules.weather.domain.services.*;
import com.danasea.backend.modules.weather.infrastructure.jobs.SlotWeatherMonitoringJob;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaSafetyRuleEvaluationRepository;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

class WeatherFreshnessSafetyGuardTest {
    WeatherInfoDto.TimeWindowForecast stale() {
        return WeatherInfoDto.TimeWindowForecast.builder()
                .sourceFetchedAt(Instant.now().minusSeconds(5400))
                .validUntil(Instant.now().minusSeconds(3600))
                .provider("OPEN_METEO")
                .peakWindSpeed(5.0)
                .peakWaveHeight(0.1)
                .build();
    }

    CheckAdvanceBookingSafetyUseCase useCase(WeatherProviderPort provider) {
        var rules = mock(CategorySafetyRuleService.class);
        when(rules.getRuleByCategorySlug(anyString()))
                .thenReturn(CategorySafetyRule.getBySlug("default"));
        return new CheckAdvanceBookingSafetyUseCase(rules, provider, new WeatherRuleEngine());
    }

    AdvanceBookingSafetyRequest request() {
        return AdvanceBookingSafetyRequest.builder()
                .categorySlug("default")
                .bookingDate(LocalDate.now().plusDays(1))
                .build();
    }

    @Test
    void oldSafeMetricsNeverCertifySafety() {
        var provider = mock(WeatherProviderPort.class);
        var stale = stale();
        when(provider.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                .thenReturn(stale);
        var result = useCase(provider).execute(request());
        assertFalse(result.isSafe());
        assertEquals("UNKNOWN", result.getSafetyStatus());
        assertTrue(result.getStale());
        assertEquals(stale.getSourceFetchedAt(), result.getFetchedAt());
    }

    @Test
    void providerTimeoutReturnsUnknownRatherThanSafeOrServerError() {
        var provider = mock(WeatherProviderPort.class);
        when(provider.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Read timeout"));
        var result = assertDoesNotThrow(() -> useCase(provider).execute(request()));
        assertFalse(result.isSafe());
        assertEquals("UNKNOWN", result.getSafetyStatus());
    }

    @Test
    void staleForecastCannotResolveExistingRedAlertInJob() {
        var slots = mock(JpaServiceSlotRepository.class);
        var services = mock(JpaServiceRepository.class);
        var categories = mock(JpaCategoryRepository.class);
        var evaluations = mock(JpaSafetyRuleEvaluationRepository.class);
        var provider = mock(WeatherProviderPort.class);
        when(provider.getTimeWindowForecast(anyDouble(), anyDouble(), any(), any(), any()))
                .thenReturn(stale());
        var service = new ServiceJpaEntity();
        service.setId(UUID.randomUUID());
        service.setWeatherSensitive(true);
        when(services.findById(service.getId())).thenReturn(Optional.of(service));
        var slot = new ServiceSlotJpaEntity();
        slot.setId(UUID.randomUUID());
        slot.setServiceId(service.getId());
        slot.setDate(LocalDate.now().plusDays(1));
        slot.setStartTime(LocalTime.NOON);
        slot.setEndTime(LocalTime.of(14, 0));
        var job =
                new SlotWeatherMonitoringJob(
                        slots,
                        services,
                        categories,
                        evaluations,
                        mock(JpaSubOrderRepository.class),
                        provider,
                        new WeatherRuleEngine(),
                        mock(SendNotificationUseCase.class));
        assertFalse(job.evaluateAndAlertSlot(slot));
        verifyNoInteractions(evaluations);
    }
}
