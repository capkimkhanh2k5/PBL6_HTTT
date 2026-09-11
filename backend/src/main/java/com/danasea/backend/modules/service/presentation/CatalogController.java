package com.danasea.backend.modules.service.presentation;

import com.danasea.backend.modules.service.application.dto.SearchServicesCriteria;
import com.danasea.backend.modules.service.application.dto.ServiceDetailResult;
import com.danasea.backend.modules.service.application.dto.ServiceSummaryResult;
import com.danasea.backend.modules.service.application.usecase.GetPublicServiceDetailUseCase;
import com.danasea.backend.modules.service.application.usecase.SearchServicesUseCase;
import com.danasea.backend.modules.service.presentation.dto.PageResponse;
import com.danasea.backend.modules.service.presentation.dto.ServiceDetailResponse;
import com.danasea.backend.modules.service.presentation.dto.ServiceSummaryResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final SearchServicesUseCase searchServicesUseCase;
    private final GetPublicServiceDetailUseCase getServiceDetailUseCase;

    @GetMapping
    public ResponseEntity<PageResponse<ServiceSummaryResponse>> searchServices(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        SearchServicesCriteria criteria = SearchServicesCriteria.builder()
                .categoryId(categoryId)
                .keyword(keyword)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .lat(lat)
                .lng(lng)
                .radiusKm(radiusKm)
                .page(page)
                .size(size)
                .build();

        List<ServiceSummaryResult> results = searchServicesUseCase.execute(criteria);
        long total = searchServicesUseCase.count(criteria);

        List<ServiceSummaryResponse> responses = results.stream().map(r -> ServiceSummaryResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .shortDescription(r.getShortDescription())
                .price(r.getPrice())
                .promotionalPrice(r.getPromotionalPrice())
                .address(r.getAddress())
                .averageRating(r.getAverageRating())
                .reviewCount(r.getReviewCount())
                .viewCount(r.getViewCount())
                .primaryImageUrl(r.getPrimaryImageUrl())
                .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(new PageResponse<>(responses, page, size, total));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceDetailResponse> getServiceDetail(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        ServiceDetailResult result = getServiceDetailUseCase.execute(id, userId, sessionId);

        ServiceDetailResponse response = ServiceDetailResponse.builder()
                .id(result.getId())
                .name(result.getName())
                .description(result.getDescription())
                .price(result.getPrice())
                .promotionalPrice(result.getPromotionalPrice())
                .address(result.getAddress())
                .latitude(result.getLatitude())
                .longitude(result.getLongitude())
                .averageRating(result.getAverageRating())
                .reviewCount(result.getReviewCount())
                .viewCount(result.getViewCount())
                .categoryId(result.getCategoryId())
                .categoryName(result.getCategoryName())
                .imageUrls(result.getImageUrls())
                .availableSlots(result.getAvailableSlots())
                .build();

        return ResponseEntity.ok(response);
    }
}
