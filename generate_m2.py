import os

base_dir = "backend/src/main/java/com/danasea/backend/modules/service"
os.makedirs(f"{base_dir}/application/dto", exist_ok=True)
os.makedirs(f"{base_dir}/application/usecase", exist_ok=True)
os.makedirs(f"{base_dir}/presentation/dto", exist_ok=True)

files = {}

files["application/dto/SearchServicesCriteria.java"] = """package com.danasea.backend.modules.service.application.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchServicesCriteria {
    private UUID categoryId;
    private String keyword;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal lat;
    private BigDecimal lng;
    private Double radiusKm;
    private int page;
    private int size;
}
"""

files["application/dto/ServiceSummaryResult.java"] = """package com.danasea.backend.modules.service.application.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceSummaryResult {
    private UUID id;
    private String name;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal promotionalPrice;
    private String address;
    private String locationDisplay;
    private BigDecimal averageRating;
    private int reviewCount;
    private int viewCount;
    private String primaryImageUrl;
}
"""

files["application/dto/ServiceDetailResult.java"] = """package com.danasea.backend.modules.service.application.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ServiceDetailResult {
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

files["application/dto/WishlistItemResult.java"] = """package com.danasea.backend.modules.service.application.dto;

import java.util.UUID;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WishlistItemResult {
    private UUID id;
    private UUID serviceId;
    private String serviceName;
    private String primaryImageUrl;
    private LocalDateTime addedAt;
}
"""

files["application/dto/RecentlyViewedResult.java"] = """package com.danasea.backend.modules.service.application.dto;

import java.util.UUID;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentlyViewedResult {
    private UUID id;
    private UUID serviceId;
    private String serviceName;
    private String primaryImageUrl;
    private LocalDateTime viewedAt;
}
"""

files["application/usecase/SearchServicesUseCase.java"] = """package com.danasea.backend.modules.service.application.usecase;

import com.danasea.backend.modules.service.application.dto.SearchServicesCriteria;
import com.danasea.backend.modules.service.application.dto.ServiceSummaryResult;
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
        return ServiceSummaryResult.Builder()
                .id(service.getId())
                .name(service.getName())
                .shortDescription(service.getShortDescription())
                .price(service.getPrice())
                .promotionalPrice(service.getPromotionalPrice())
                .address(service.getAddress())
                .averageRating(service.getAverageRating())
                .reviewCount(service.getReviewCount())
                .viewCount(service.getViewCount())
                // Assuming first image is primary if exists (mocking logic)
                .primaryImageUrl(service.getImages() != null && !service.getImages().isEmpty() ? service.getImages().get(0).getImageUrl() : null)
                .build();
    }
}
"""

files["application/usecase/RecordRecentlyViewedUseCase.java"] = """package com.danasea.backend.modules.service.application.usecase;

import com.danasea.backend.modules.service.domain.models.RecentlyViewed;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.RecentlyViewedRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecordRecentlyViewedUseCase {
    private final RecentlyViewedRepositoryPort recentlyViewedRepositoryPort;
    private final ServiceRepositoryPort serviceRepositoryPort;

    public void execute(UUID serviceId, UUID userId, String sessionId) {
        if (userId == null && (sessionId == null || sessionId.trim().isEmpty())) {
            return;
        }
        
        Optional<Service> serviceOpt = serviceRepositoryPort.findById(serviceId);
        if (serviceOpt.isEmpty()) return;
        Service service = serviceOpt.get();

        Optional<RecentlyViewed> existing = recentlyViewedRepositoryPort.findByUserIdOrSessionIdAndServiceId(userId, sessionId, serviceId);
        
        if (existing.isPresent()) {
            RecentlyViewed rv = existing.get();
            rv.setViewedAt(LocalDateTime.now());
            recentlyViewedRepositoryPort.save(rv);
        } else {
            RecentlyViewed rv = new RecentlyViewed();
            rv.setId(UUID.randomUUID());
            rv.setService(service);
            rv.setUserId(userId);
            rv.setSessionId(sessionId);
            rv.setViewedAt(LocalDateTime.now());
            recentlyViewedRepositoryPort.save(rv);
        }
    }
}
"""

files["application/usecase/GetServiceDetailUseCase.java"] = """package com.danasea.backend.modules.service.application.usecase;

import com.danasea.backend.modules.service.application.dto.ServiceDetailResult;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceImage;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetServiceDetailUseCase {
    private final ServiceRepositoryPort serviceRepositoryPort;
    private final RecordRecentlyViewedUseCase recordRecentlyViewedUseCase;

    public ServiceDetailResult execute(UUID id, UUID userId, String sessionId) {
        Service service = serviceRepositoryPort.findPublishedById(id)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found or not published: " + id));

        serviceRepositoryPort.incrementViewCount(id, ServiceStatus.PUBLISHED);

        recordRecentlyViewedUseCase.execute(id, userId, sessionId);

        return ServiceDetailResult.Builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .promotionalPrice(service.getPromotionalPrice())
                .address(service.getAddress())
                .latitude(service.getLatitude())
                .longitude(service.getLongitude())
                .averageRating(service.getAverageRating())
                .reviewCount(service.getReviewCount())
                // We add 1 manually here because the DB increment might not be reflected in the JPA entity fetched before it
                .viewCount(service.getViewCount() + 1) 
                .categoryId(service.getCategory() != null ? service.getCategory().getId() : null)
                .categoryName(service.getCategory() != null ? service.getCategory().getName() : null)
                .imageUrls(service.getImages() != null ? service.getImages().stream().map(ServiceImage::getImageUrl).collect(Collectors.toList()) : null)
                .build();
    }
}
"""

files["application/usecase/WishlistUseCase.java"] = """package com.danasea.backend.modules.service.application.usecase;

import com.danasea.backend.modules.service.application.dto.WishlistItemResult;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.Wishlist;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.WishlistRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WishlistUseCase {
    private final WishlistRepositoryPort wishlistRepositoryPort;
    private final ServiceRepositoryPort serviceRepositoryPort;

    public void addWishlist(UUID userId, UUID serviceId) {
        if (!serviceRepositoryPort.existsById(serviceId)) {
            throw new ServiceNotFoundException("Service not found: " + serviceId);
        }

        if (wishlistRepositoryPort.existsByUserIdAndServiceId(userId, serviceId)) {
            return; // Idempotent
        }

        Service service = serviceRepositoryPort.findById(serviceId).get();
        Wishlist wishlist = new Wishlist();
        wishlist.setId(UUID.randomUUID());
        wishlist.setUserId(userId);
        wishlist.setService(service);
        wishlist.setCreatedAt(LocalDateTime.now());
        
        wishlistRepositoryPort.save(wishlist);
    }

    public void removeWishlist(UUID userId, UUID serviceId) {
        wishlistRepositoryPort.deleteByUserIdAndServiceId(userId, serviceId); // Idempotent by design
    }

    public List<WishlistItemResult> getWishlists(UUID userId) {
        return wishlistRepositoryPort.findByUserId(userId).stream().map(w -> 
            WishlistItemResult.Builder()
                .id(w.getId())
                .serviceId(w.getService().getId())
                .serviceName(w.getService().getName())
                .primaryImageUrl(w.getService().getImages() != null && !w.getService().getImages().isEmpty() ? w.getService().getImages().get(0).getImageUrl() : null)
                .addedAt(w.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }
}
"""

for path, content in files.items():
    with open(f"{base_dir}/{path}", "w") as f:
        f.write(content)

print("Generated Application layer")
