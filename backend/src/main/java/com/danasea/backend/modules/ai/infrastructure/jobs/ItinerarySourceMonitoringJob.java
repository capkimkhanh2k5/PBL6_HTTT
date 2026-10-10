package com.danasea.backend.modules.ai.infrastructure.jobs;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.danasea.backend.modules.ai.application.port.ItineraryStorePort;
import com.danasea.backend.modules.ai.application.usecase.PlanItineraryUseCase;
import com.danasea.backend.modules.service.application.api.AiItinerarySlotReadApi;
import com.danasea.backend.modules.weather.application.api.AiItineraryAlertReadApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = {"app.scheduler.enabled", "ai.itinerary.monitoring-enabled"}, havingValue = "true", matchIfMissing = true)
public class ItinerarySourceMonitoringJob {
    private final ItineraryStorePort store;
    private final AiItinerarySlotReadApi slots;
    private final AiItineraryAlertReadApi alerts;
    private final PlanItineraryUseCase planner;
    private int page;

    @Scheduled(fixedDelayString = "${ai.itinerary.monitoring-delay-ms:60000}")
    public void check() {
        List<UUID> ids = store.trackedSlotIds(page, 100);
        page = ids.size() < 100 ? 0 : page + 1;
        if (ids.isEmpty()) return;
        var slotIds = slots.futureSlotIds(new HashSet<>(ids));
        if (slotIds.isEmpty()) return;
        slots.unavailableSignals(slotIds).forEach(signal -> apply(signal.slotId(), signal.eventId(), signal.trigger()));
        alerts.activeSignals(slotIds).forEach(signal -> apply(signal.slotId(), signal.eventId(), signal.trigger()));
    }

    private void apply(UUID slotId, String eventId, String trigger) {
        try { planner.sourceChanged(slotId, eventId, trigger); }
        catch (RuntimeException exception) {
            log.warn("Itinerary source event could not be processed; event {} will be retried", eventId);
        }
    }
}
