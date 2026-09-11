package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import com.danasea.backend.modules.service.domain.models.Wishlist;
import com.danasea.backend.modules.service.infrastructure.mapper.WishlistMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.WishlistJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaWishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WishlistRepositoryAdapterTest {

    private JpaWishlistRepository jpaWishlistRepository;
    private WishlistMapper wishlistMapper;
    private WishlistRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpaWishlistRepository = mock(JpaWishlistRepository.class);
        wishlistMapper = mock(WishlistMapper.class);
        adapter = new WishlistRepositoryAdapter(jpaWishlistRepository, wishlistMapper);
    }

    @Test
    void existsByUserIdAndServiceId_delegates() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(jpaWishlistRepository.existsByUserIdAndServiceId(userId, serviceId)).thenReturn(true);

        assertTrue(adapter.existsByUserIdAndServiceId(userId, serviceId));
    }

    @Test
    void deleteByUserIdAndServiceId_delegates() {
        UUID userId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();

        adapter.deleteByUserIdAndServiceId(userId, serviceId);

        verify(jpaWishlistRepository).deleteByUserIdAndServiceId(userId, serviceId);
    }

    @Test
    void findAllByUserId_returnsMappedList() {
        UUID userId = UUID.randomUUID();
        WishlistJpaEntity entity = new WishlistJpaEntity();
        Wishlist domain = new Wishlist();

        when(jpaWishlistRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(entity));
        when(wishlistMapper.toDomain(entity)).thenReturn(domain);

        List<Wishlist> result = adapter.findAllByUserId(userId);

        assertEquals(1, result.size());
        assertEquals(domain, result.get(0));
    }

    @Test
    void save_mapsAndSaves() {
        Wishlist domain = new Wishlist();
        WishlistJpaEntity entity = new WishlistJpaEntity();

        when(wishlistMapper.toEntity(domain)).thenReturn(entity);
        when(jpaWishlistRepository.save(entity)).thenReturn(entity);
        when(wishlistMapper.toDomain(entity)).thenReturn(domain);

        Wishlist saved = adapter.save(domain);

        assertNotNull(saved);
        verify(jpaWishlistRepository).save(entity);
    }

    @Test
    void findById_returnsMappedDomain() {
        UUID id = UUID.randomUUID();
        WishlistJpaEntity entity = new WishlistJpaEntity();
        Wishlist domain = new Wishlist();

        when(jpaWishlistRepository.findById(id)).thenReturn(Optional.of(entity));
        when(wishlistMapper.toDomain(entity)).thenReturn(domain);

        Optional<Wishlist> result = adapter.findById(id);

        assertTrue(result.isPresent());
    }
}
