import os

base_dir = "backend/src/main/java/com/danasea/backend/modules/service"
test_dir = "backend/src/test/java/com/danasea/backend/modules/service"

files = {}

# Use Cases
files["application/usecase/RecordRecentlyViewedUseCase.java"] = """package com.danasea.backend.modules.service.application.usecase;

import com.danasea.backend.modules.service.domain.models.RecentlyViewed;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.RecentlyViewedRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
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

        Optional<RecentlyViewed> existing = Optional.empty();
        if (userId != null) {
            existing = recentlyViewedRepositoryPort.findFirstByUserIdAndServiceId(userId, serviceId);
        } else if (sessionId != null && !sessionId.trim().isEmpty()) {
            existing = recentlyViewedRepositoryPort.findFirstBySessionIdAndServiceId(sessionId, serviceId);
        }
        
        if (existing.isPresent()) {
            RecentlyViewed rv = existing.get();
            rv.setViewedAt(OffsetDateTime.now());
            recentlyViewedRepositoryPort.save(rv);
        } else {
            RecentlyViewed rv = new RecentlyViewed();
            rv.setId(UUID.randomUUID());
            rv.setServiceId(serviceId);
            rv.setUserId(userId);
            rv.setSessionId(sessionId);
            rv.setViewedAt(OffsetDateTime.now());
            recentlyViewedRepositoryPort.save(rv);
        }
    }
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
"""

files["application/usecase/GetServiceDetailUseCase.java"] = """package com.danasea.backend.modules.service.application.usecase;

import com.danasea.backend.modules.service.application.dto.ServiceDetailResult;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

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

        int viewCount = service.getViewCount() != null ? service.getViewCount() : 0;
        return ServiceDetailResult.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .address(service.getAddress())
                .latitude(service.getLatitude())
                .longitude(service.getLongitude())
                .averageRating(service.getAvgRating())
                .reviewCount(service.getRatingCount() != null ? service.getRatingCount() : 0)
                .viewCount(viewCount + 1)
                .categoryId(service.getCategoryId())
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

import java.time.OffsetDateTime;
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

        Wishlist wishlist = new Wishlist();
        wishlist.setId(UUID.randomUUID());
        wishlist.setUserId(userId);
        wishlist.setServiceId(serviceId);
        wishlist.setCreatedAt(OffsetDateTime.now());
        
        wishlistRepositoryPort.save(wishlist);
    }

    public void removeWishlist(UUID userId, UUID serviceId) {
        wishlistRepositoryPort.deleteByUserIdAndServiceId(userId, serviceId); // Idempotent by design
    }

    public List<WishlistItemResult> getWishlists(UUID userId) {
        return wishlistRepositoryPort.findAllByUserId(userId).stream().map(w -> {
            Service service = serviceRepositoryPort.findById(w.getServiceId()).orElse(new Service());
            LocalDateTime added = w.getCreatedAt() != null ? w.getCreatedAt().toLocalDateTime() : null;
            return WishlistItemResult.builder()
                .id(w.getId())
                .serviceId(w.getServiceId())
                .serviceName(service.getName())
                .addedAt(added)
                .build();
        }).collect(Collectors.toList());
    }
}
"""

files["application/usecase/GetRecentlyViewedUseCase.java"] = """package com.danasea.backend.modules.service.application.usecase;

import com.danasea.backend.modules.service.application.dto.RecentlyViewedResult;
import com.danasea.backend.modules.service.domain.models.RecentlyViewed;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.RecentlyViewedRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetRecentlyViewedUseCase {
    private final RecentlyViewedRepositoryPort recentlyViewedRepositoryPort;
    private final ServiceRepositoryPort serviceRepositoryPort;

    public List<RecentlyViewedResult> execute(UUID userId, String sessionId) {
        List<RecentlyViewed> entities;
        if (userId != null) {
            entities = recentlyViewedRepositoryPort.findAllByUserId(userId);
        } else if (sessionId != null && !sessionId.trim().isEmpty()) {
            entities = recentlyViewedRepositoryPort.findAllBySessionId(sessionId);
        } else {
            return List.of();
        }

        return entities.stream().map(rv -> {
            Service service = serviceRepositoryPort.findById(rv.getServiceId()).orElse(new Service());
            return RecentlyViewedResult.builder()
                .id(rv.getId())
                .serviceId(rv.getServiceId())
                .serviceName(service.getName())
                .viewedAt(rv.getViewedAt() != null ? rv.getViewedAt().toLocalDateTime() : null)
                .build();
        }).collect(Collectors.toList());
    }
}
"""

for path, content in files.items():
    with open(f"{base_dir}/{path}", "w") as f:
        f.write(content)

test_files = {}
test_files["application/usecase/RecordRecentlyViewedUseCaseTest.java"] = """package com.danasea.backend.modules.service.application.usecase;

import com.danasea.backend.modules.service.domain.models.RecentlyViewed;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.RecentlyViewedRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordRecentlyViewedUseCaseTest {

    @Mock
    private RecentlyViewedRepositoryPort recentlyViewedRepositoryPort;

    @Mock
    private ServiceRepositoryPort serviceRepositoryPort;

    @InjectMocks
    private RecordRecentlyViewedUseCase recordRecentlyViewedUseCase;

    @Test
    void execute_WithExistingRecord_ShouldUpdateViewedAt() {
        UUID serviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Service service = new Service();
        service.setId(serviceId);
        
        RecentlyViewed rv = new RecentlyViewed();
        rv.setId(UUID.randomUUID());

        when(serviceRepositoryPort.findById(serviceId)).thenReturn(Optional.of(service));
        when(recentlyViewedRepositoryPort.findFirstByUserIdAndServiceId(userId, serviceId))
                .thenReturn(Optional.of(rv));

        recordRecentlyViewedUseCase.execute(serviceId, userId, null);

        verify(recentlyViewedRepositoryPort, times(1)).save(rv);
    }

    @Test
    void execute_WithNewRecord_ShouldInsert() {
        UUID serviceId = UUID.randomUUID();
        String sessionId = "sess1";
        
        Service service = new Service();
        service.setId(serviceId);

        when(serviceRepositoryPort.findById(serviceId)).thenReturn(Optional.of(service));
        when(recentlyViewedRepositoryPort.findFirstBySessionIdAndServiceId(sessionId, serviceId))
                .thenReturn(Optional.empty());

        recordRecentlyViewedUseCase.execute(serviceId, null, sessionId);

        verify(recentlyViewedRepositoryPort, times(1)).save(any(RecentlyViewed.class));
    }
}
"""

for path, content in test_files.items():
    with open(f"{test_dir}/{path}", "w") as f:
        f.write(content)

print("Fixed compilations")
