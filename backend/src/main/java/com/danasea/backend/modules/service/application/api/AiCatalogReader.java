package com.danasea.backend.modules.service.application.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.service.application.usecases.GetServiceSlotsAvailabilityUseCase;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.models.OptionStatus;
import com.danasea.backend.modules.service.domain.models.PricingUnit;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceOptionRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.shared.i18n.SupportedLanguage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiCatalogReader implements AiCatalogReadApi {
    private final ServiceRepositoryPort services;
    private final ServiceOptionRepositoryPort options;
    private final CategoryRepositoryPort categories;
    private final GetServiceSlotsAvailabilityUseCase availability;

    @Override
    @Transactional(readOnly = true)
    public List<PublishedService> search(Query query) {
        return services.searchPublishedServices(query.categoryId(), query.keyword(), null, null,
                decimal(query.latitude()), decimal(query.longitude()), query.radiusKm(), 0,
                Math.min(50, Math.max(1, query.limit()))).stream().map(service -> snapshot(service, query, true)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PublishedService find(UUID id, Query query) {
        return snapshot(services.findPublishedById(id)
                .orElseThrow(() -> new ServiceNotFoundException("Published service not found: " + id)), query, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PublishedService metadata(UUID id, SupportedLanguage language) {
        return snapshot(services.findPublishedById(id).orElseThrow(() -> new ServiceNotFoundException("Published service not found: " + id)),
                new Query(null, null, null, null, 1, null, null, null, 1, language), false);
    }

    private PublishedService snapshot(Service service, Query query, boolean withSlots) {
        var category = service.getCategoryId() == null ? null : categories.findById(service.getCategoryId()).orElse(null);
        List<Option> quotedOptions = options.findByServiceIdAndStatus(service.getId(), OptionStatus.ACTIVE).stream()
                .filter(option -> option.getPrice() != null && option.getPrice().signum() >= 0 && option.getPricingUnit() != null)
                .filter(option -> option.getPricingUnit() != PricingUnit.PER_PACKAGE
                        || option.getMaxPaxPerPackage() != null && option.getMaxPaxPerPackage() > 0)
                .map(option -> {
                    int quantity = option.getPricingUnit() == PricingUnit.PER_PACKAGE
                            ? (query.partySize() - 1) / option.getMaxPaxPerPackage() + 1
                            : query.partySize();
                    List<Slot> slots = !withSlots ? List.of() : availability.execute(service.getId(), option.getId(), query.from(), query.to(), quantity, false)
                            .stream().filter(slot -> slot.bookable()).map(slot -> new Slot(slot.slotId(), slot.date(),
                                    slot.startTime(), slot.endTime(), slot.availablePaxOrPackages())).toList();
                    return new Option(option.getId(), option.getName(), option.getPricingUnit().name(), option.getPrice(),
                            quantity, option.getPrice().multiply(BigDecimal.valueOf(quantity)), option.getBenefits(), slots);
                }).toList();
        boolean english = query.language() == SupportedLanguage.EN;
        return new PublishedService(service.getId(), service.getVendorId(),
                localize(service.getName(), service.getNameEn(), english),
                localize(service.getDescription(), service.getDescriptionEn(), english), service.getCategoryId(),
                category == null ? null : localize(category.getName(), category.getNameEn(), english),
                category == null ? null : category.getSlug(), service.getAddress(),
                number(service.getLatitude()), number(service.getLongitude()), service.getDurationMinutes(),
                Boolean.TRUE.equals(service.getWeatherSensitive()), service.getAvgRating(),
                service.getRatingCount() == null ? 0 : service.getRatingCount(), quotedOptions);
    }

    private String localize(String vietnamese, String english, boolean useEnglish) {
        return useEnglish && english != null && !english.isBlank() ? english : vietnamese;
    }

    private BigDecimal decimal(Double value) { return value == null ? null : BigDecimal.valueOf(value); }
    private Double number(BigDecimal value) { return value == null ? null : value.doubleValue(); }
}
