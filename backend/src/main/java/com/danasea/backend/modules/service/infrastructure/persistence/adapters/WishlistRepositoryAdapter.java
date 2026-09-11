package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.Wishlist;
import com.danasea.backend.modules.service.domain.ports.WishlistRepositoryPort;
import com.danasea.backend.modules.service.infrastructure.mapper.WishlistMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.WishlistJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaWishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WishlistRepositoryAdapter implements WishlistRepositoryPort {

    private final JpaWishlistRepository jpaWishlistRepository;
    private final WishlistMapper wishlistMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndServiceId(UUID userId, UUID serviceId) {
        return jpaWishlistRepository.existsByUserIdAndServiceId(userId, serviceId);
    }

    @Override
    @Transactional
    public void deleteByUserIdAndServiceId(UUID userId, UUID serviceId) {
        jpaWishlistRepository.deleteByUserIdAndServiceId(userId, serviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Wishlist> findAllByUserId(UUID userId) {
        return jpaWishlistRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(wishlistMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Wishlist save(Wishlist wishlist) {
        WishlistJpaEntity entity = wishlistMapper.toEntity(wishlist);
        WishlistJpaEntity saved = jpaWishlistRepository.save(entity);
        return wishlistMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Wishlist> findById(UUID id) {
        return jpaWishlistRepository.findById(id)
                .map(wishlistMapper::toDomain);
    }
}
