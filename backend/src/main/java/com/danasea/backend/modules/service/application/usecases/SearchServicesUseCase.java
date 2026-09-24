package com.danasea.backend.modules.service.application.usecases;

import com.danasea.backend.modules.service.application.dtos.SearchServicesCriteria;
import com.danasea.backend.modules.service.application.dtos.ServiceSummaryResult;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchServicesUseCase {
    private final ServiceRepositoryPort serviceRepositoryPort;
    private final ServiceImageRepositoryPort serviceImageRepositoryPort;

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
        String primaryImageUrl = serviceImageRepositoryPort.findByServiceId(service.getId()).stream()
                .findFirst()
                .map(image -> image.getUrl())
                .orElse(null);
        return ServiceSummaryResult.builder()
                .id(service.getId())
                .name(service.getName())
                .shortDescription(shortDescription(service.getDescription()))
                .price(service.getPrice())
                .address(service.getAddress())
                .averageRating(service.getAvgRating())
                .reviewCount(service.getRatingCount() != null ? service.getRatingCount() : 0)
                .viewCount(service.getViewCount() != null ? service.getViewCount() : 0)
                .primaryImageUrl(primaryImageUrl)
                .build();
    }

    private String shortDescription(String description) {
        if (description == null || description.length() <= 180) {
            return description;
        }
        return description.substring(0, 177) + "...";
    }
}
