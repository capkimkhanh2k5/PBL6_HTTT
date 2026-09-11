package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.service.infrastructure.persistence.entities.WishlistJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaWishlistRepository extends JpaRepository<WishlistJpaEntity, UUID> {

    boolean existsByUserIdAndServiceId(UUID userId, UUID serviceId);

    void deleteByUserIdAndServiceId(UUID userId, UUID serviceId);

    List<WishlistJpaEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}

