package com.danasea.backend.modules.ai.application.usecase;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Slot;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.ai.application.port.TravelWeatherPort;
import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.shared.i18n.SupportedLanguage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeatherAwareUseCase {
    public record Alternative(UUID optionId, Slot slot, TravelWeatherPort.Assessment assessment) {}
    public record Result(UUID serviceId, UUID optionId, UUID slotId, TravelWeatherPort.Assessment assessment,
                         boolean bookingAuthorized, List<Alternative> alternatives, List<String> limitations) {}
    private final DiscoverServicesUseCase discovery;
    private final TravelWeatherPort weather;

    public Result execute(UUID serviceId, UUID optionId, UUID slotId, TravelContext context, SupportedLanguage language) {
        context.validateCurrentDates();
        if (serviceId == null || optionId == null || slotId == null) throw new IllegalArgumentException("Service, option and slot IDs are required");
        var service = discovery.detail(serviceId, context, language);
        var option = service.options().stream().filter(candidate -> candidate.id().equals(optionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The active option does not belong to the service"));
        var slot = option.slots().stream().filter(candidate -> candidate.id().equals(slotId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The slot is not currently bookable for the party and date range"));
        var assessment = weather.assess(service, slot);
        List<Alternative> alternatives = new ArrayList<>();
        if (!assessment.acceptable() || assessment.provisional()) {
            int checked = 0;
            for (Slot candidate : option.slots()) {
                if (candidate.id().equals(slotId)) continue;
                if (checked++ >= 6 || alternatives.size() >= 3) break;
                var alternative = weather.assess(service, candidate);
                if (alternative.acceptable()) alternatives.add(new Alternative(optionId, candidate, alternative));
            }
        }
        return new Result(serviceId, optionId, slotId, assessment, false, List.copyOf(alternatives),
                List.of("ALTERNATIVES_WITHIN_REQUESTED_DATE_RANGE", "REVALIDATE_WEATHER_AND_INVENTORY_BEFORE_BOOKING"));
    }
}
