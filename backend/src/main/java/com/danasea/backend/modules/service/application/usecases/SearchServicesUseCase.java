package com.danasea.backend.modules.service.application.usecases;

import com.danasea.backend.modules.service.application.dtos.SearchServicesCriteria;
import com.danasea.backend.modules.service.application.dtos.ServiceSummaryResult;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchServicesUseCase {
    private final ServiceRepositoryPort serviceRepositoryPort;

    public List<ServiceSummaryResult> execute(SearchServicesCriteria criteria) {
        List<Service> services = serviceRepositoryPort.searchPublishedServices(
                criteria.getCategoryId(),
                criteria.getKeyword(),
                criteria.getMinPrice(),
                criteria.getMaxPrice(),
                criteria.getLat(),
                criteria.getLng(),
                criteria.getRadiusKm(),
                criteria.getPage(),
                criteria.getSize()
        );

        return services.stream().map(this::mapToResult).collect(Collectors.toList());
    }

    public long count(SearchServicesCriteria criteria) {
        return serviceRepositoryPort.countPublishedServices(
                criteria.getCategoryId(),
                criteria.getKeyword(),
                criteria.getMinPrice(),
                criteria.getMaxPrice(),
                criteria.getLat(),
                criteria.getLng(),
                criteria.getRadiusKm()
        );
    }

    private ServiceSummaryResult mapToResult(Service service) {
        return ServiceSummaryResult.builder()
                .id(service.getId())
                .name(service.getName())
                .price(service.getPrice())
                .address(service.getAddress())
                .averageRating(service.getAvgRating())
                .reviewCount(service.getRatingCount() != null ? service.getRatingCount() : 0)
                .viewCount(service.getViewCount() != null ? service.getViewCount() : 0)
                .build();
    }
}
