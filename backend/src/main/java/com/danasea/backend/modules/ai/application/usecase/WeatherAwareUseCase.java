package com.danasea.backend.modules.ai.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.ai.application.port.TravelWeatherPort;
import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.shared.i18n.SupportedLanguage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeatherAwareUseCase {
    public record Result(UUID serviceId, UUID optionId, UUID slotId, TravelWeatherPort.Assessment assessment,
                         boolean bookingAuthorized) {}
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
        return new Result(serviceId, optionId, slotId, weather.assess(service, slot), false);
    }
}
