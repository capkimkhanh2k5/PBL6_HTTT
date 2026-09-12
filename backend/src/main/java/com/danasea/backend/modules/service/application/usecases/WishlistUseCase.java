package com.danasea.backend.modules.service.application.usecases;

import com.danasea.backend.modules.service.application.dtos.WishlistItemResult;
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
