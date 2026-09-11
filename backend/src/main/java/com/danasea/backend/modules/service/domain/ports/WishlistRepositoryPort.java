package com.danasea.backend.modules.service.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.Wishlist;

public interface WishlistRepositoryPort {

    boolean existsByUserIdAndServiceId(UUID userId, UUID serviceId);

    void deleteByUserIdAndServiceId(UUID userId, UUID serviceId);

    List<Wishlist> findAllByUserId(UUID userId);

    Wishlist save(Wishlist wishlist);

    Optional<Wishlist> findById(UUID id);
}
