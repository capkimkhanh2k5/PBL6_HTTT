package com.danasea.backend.modules.weather.infrastructure.persistence.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.danasea.backend.modules.weather.infrastructure.persistence.entities.CategorySafetyRuleJpaEntity;

@Repository
public interface JpaCategorySafetyRuleRepository extends JpaRepository<CategorySafetyRuleJpaEntity, UUID> {

    Optional<CategorySafetyRuleJpaEntity> findByCategoryId(UUID categoryId);

    Optional<CategorySafetyRuleJpaEntity> findByCategorySlug(String categorySlug);

    boolean existsByCategorySlug(String categorySlug);
}
