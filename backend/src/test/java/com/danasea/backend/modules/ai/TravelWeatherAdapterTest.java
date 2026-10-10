package com.danasea.backend.modules.ai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import com.danasea.backend.modules.ai.infrastructure.adapters.TravelWeatherAdapter;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.PublishedService;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Slot;
import com.danasea.backend.modules.weather.application.dtos.AdvanceBookingSafetyResponse;
import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto.TimeWindowForecast;
import com.danasea.backend.modules.weather.application.usecases.CheckAdvanceBookingSafetyUseCase;

class TravelWeatherAdapterTest {
    @Test void estimatedMarineIsProvisionalAndMissingForecastCannotBeSafe() {
        var source = mock(CheckAdvanceBookingSafetyUseCase.class);
        var adapter = new TravelWeatherAdapter(source);
        var service = new PublishedService(UUID.randomUUID(), null, "Kayak", "Trip", null, null, null, "Beach", 16.1, 108.2,
                60, true, null, 0, List.of());
        var slot = new Slot(UUID.randomUUID(), LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0), 10);
        when(source.checkBySlotId(slot.id())).thenReturn(AdvanceBookingSafetyResponse.builder().isSafe(true).isProvisional(false)
                .estimatedMarine(true).forecast(TimeWindowForecast.builder().peakWindSpeed(10.0).sourceFetchedAt(Instant.now()).validUntil(Instant.now().plusSeconds(60)).build()).build());
        var estimated = adapter.assess(service, slot);
        assertThat(estimated.acceptable()).isTrue(); assertThat(estimated.provisional()).isTrue();
        when(source.checkBySlotId(slot.id())).thenReturn(AdvanceBookingSafetyResponse.builder().isSafe(true).forecast(null).build());
        assertThat(adapter.assess(service, slot).status()).isEqualTo("UNKNOWN");
        assertThat(adapter.assess(service, slot).acceptable()).isFalse();
    }
}
