import os

base_dir = "backend/src/main/java/com/danasea/backend/modules/service/presentation"
os.makedirs(f"{base_dir}/dto", exist_ok=True)

files = {}

files["dto/ServiceSummaryResponse.java"] = """package com.danasea.backend.modules.service.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceSummaryResponse {
    private UUID id;
    private String name;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal promotionalPrice;
    private String address;
    private BigDecimal averageRating;
    private int reviewCount;
    private int viewCount;
    private String primaryImageUrl;
}
"""

files["dto/ServiceDetailResponse.java"] = """package com.danasea.backend.modules.service.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ServiceDetailResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal promotionalPrice;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal averageRating;
    private int reviewCount;
    private int viewCount;
    private UUID categoryId;
    private String categoryName;
    private List<String> imageUrls;
    private List<String> availableSlots;
}
"""

files["dto/WishlistItemResponse.java"] = """package com.danasea.backend.modules.service.presentation.dto;

import java.util.UUID;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WishlistItemResponse {
    private UUID id;
    private UUID serviceId;
    private String serviceName;
    private String primaryImageUrl;
    private LocalDateTime addedAt;
}
"""

files["dto/RecentlyViewedResponse.java"] = """package com.danasea.backend.modules.service.presentation.dto;

import java.util.UUID;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentlyViewedResponse {
    private UUID id;
    private UUID serviceId;
    private String serviceName;
    private String primaryImageUrl;
    private LocalDateTime viewedAt;
}
"""

files["dto/PageResponse.java"] = """package com.danasea.backend.modules.service.presentation.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
}
"""

files["CatalogController.java"] = """package com.danasea.backend.modules.service.presentation;

import com.danasea.backend.modules.service.application.dto.SearchServicesCriteria;
import com.danasea.backend.modules.service.application.dto.ServiceDetailResult;
import com.danasea.backend.modules.service.application.dto.ServiceSummaryResult;
import com.danasea.backend.modules.service.application.usecase.GetServiceDetailUseCase;
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
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class CatalogController {

    private final SearchServicesUseCase searchServicesUseCase;
    private final GetServiceDetailUseCase getServiceDetailUseCase;

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
"""

files["WishlistController.java"] = """package com.danasea.backend.modules.service.presentation;

import com.danasea.backend.modules.service.application.dto.WishlistItemResult;
import com.danasea.backend.modules.service.application.usecase.WishlistUseCase;
import com.danasea.backend.modules.service.presentation.dto.WishlistItemResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistUseCase wishlistUseCase;

    @PostMapping("/{serviceId}")
    public ResponseEntity<Void> addWishlist(@PathVariable UUID serviceId) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized")); // Let global handler handle this, or create CustomException
        
        wishlistUseCase.addWishlist(userId, serviceId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> removeWishlist(@PathVariable UUID serviceId) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        
        wishlistUseCase.removeWishlist(userId, serviceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getWishlists() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        List<WishlistItemResult> results = wishlistUseCase.getWishlists(userId);
        List<WishlistItemResponse> responses = results.stream().map(r -> WishlistItemResponse.builder()
                .id(r.getId())
                .serviceId(r.getServiceId())
                .serviceName(r.getServiceName())
                .primaryImageUrl(r.getPrimaryImageUrl())
                .addedAt(r.getAddedAt())
                .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
"""

files["RecentlyViewedController.java"] = """package com.danasea.backend.modules.service.presentation;

import com.danasea.backend.modules.service.application.dto.RecentlyViewedResult;
import com.danasea.backend.modules.service.application.usecase.GetRecentlyViewedUseCase;
import com.danasea.backend.modules.service.presentation.dto.RecentlyViewedResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recently-viewed")
@RequiredArgsConstructor
public class RecentlyViewedController {

    private final GetRecentlyViewedUseCase getRecentlyViewedUseCase;

    @GetMapping
    public ResponseEntity<List<RecentlyViewedResponse>> getRecentlyViewed(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        
        List<RecentlyViewedResult> results = getRecentlyViewedUseCase.execute(userId, sessionId);
        
        List<RecentlyViewedResponse> responses = results.stream().map(r -> RecentlyViewedResponse.builder()
                .id(r.getId())
                .serviceId(r.getServiceId())
                .serviceName(r.getServiceName())
                .primaryImageUrl(r.getPrimaryImageUrl())
                .viewedAt(r.getViewedAt())
                .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
"""

files["CatalogExceptionHandler.java"] = """package com.danasea.backend.modules.service.presentation;

import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class CatalogExceptionHandler {

    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleServiceNotFound(ServiceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "code", "SERVICE_NOT_FOUND",
                "message", ex.getMessage()
        ));
    }
}
"""

for path, content in files.items():
    with open(f"{base_dir}/{path}", "w") as f:
        f.write(content)

print("Generated Presentation layer")
