package com.danasea.backend.modules.ai.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.ai.application.port.TravelPolicyPort;
import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.modules.order.application.api.AiPolicyReadApi.Snapshot;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.PublishedService;
import com.danasea.backend.shared.i18n.SupportedLanguage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompareServicesUseCase {
    public record Result(List<PublishedService> services, List<String> comparedFields,
                         String priceBasis, Snapshot commonPolicy, boolean inventoryReserved) {}
    private final DiscoverServicesUseCase discovery;
    private final TravelPolicyPort policies;

    public Result execute(List<UUID> serviceIds, TravelContext context, SupportedLanguage language) {
        context.validateCurrentDates();
        if (serviceIds == null || serviceIds.size() < 2 || serviceIds.size() > 4
                || serviceIds.stream().anyMatch(id -> id == null) || serviceIds.stream().distinct().count() != serviceIds.size()) {
            throw new IllegalArgumentException("Provide two to four distinct service IDs");
        }
        return new Result(serviceIds.stream().map(id -> discovery.detail(id, context, language)).toList(),
                List.of("ACTIVE_OPTION_PRICE", "PARTY_TOTAL", "BENEFITS", "DURATION", "RATING", "REVIEW_COUNT", "CURRENT_SLOTS", "LOCATION", "COMMON_PLATFORM_POLICY"),
                "CURRENT_ACTIVE_OPTION_QUOTE_FOR_PARTY_IN_VND", policies.current(), false);
    }
}
