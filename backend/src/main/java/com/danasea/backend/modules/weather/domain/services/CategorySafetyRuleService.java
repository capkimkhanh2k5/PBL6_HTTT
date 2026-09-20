package com.danasea.backend.modules.weather.domain.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.weather.application.dtos.CategorySafetyRuleResponse;
import com.danasea.backend.modules.weather.application.dtos.UpdateCategorySafetyRuleRequest;
import com.danasea.backend.modules.weather.domain.exceptions.CategorySafetyRuleNotFoundException;
import com.danasea.backend.modules.weather.domain.exceptions.InvalidSafetyRuleThresholdException;
import com.danasea.backend.modules.weather.domain.models.CategorySafetyRule;
import com.danasea.backend.modules.weather.infrastructure.persistence.entities.CategorySafetyRuleJpaEntity;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaCategorySafetyRuleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategorySafetyRuleService {

    private final JpaCategorySafetyRuleRepository repository;

    @Cacheable(value = "categorySafetyRules", key = "'all'")
    public List<CategorySafetyRule> getAllRules() {
        try {
            List<CategorySafetyRuleJpaEntity> entities = repository.findAll();
            if (entities != null && !entities.isEmpty()) {
                return entities.stream().map(this::mapToDomain).collect(Collectors.toList());
            }
            log.warn("Category safety rules table is empty in database. Falling back to built-in registry.");
        } catch (DataAccessException ex) {
            log.error("Database access error querying category safety rules. Falling back to built-in registry. Error: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error querying category safety rules. Falling back to built-in registry. Error: {}", ex.getMessage());
        }

        return new ArrayList<>(CategorySafetyRule.REGISTRY.values());
    }

    @Cacheable(value = "categorySafetyRules", key = "#slug")
    public CategorySafetyRule getRuleByCategorySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return CategorySafetyRule.DEFAULT_RULE;
        }

        try {
            var entityOpt = repository.findByCategorySlug(slug.toLowerCase().trim());
            if (entityOpt.isPresent()) {
                return mapToDomain(entityOpt.get());
            }
            log.warn("Rule for category slug '{}' not found in database. Falling back to registry.", slug);
        } catch (DataAccessException ex) {
            log.error("Database error retrieving category safety rule for slug '{}'. Falling back to registry. Error: {}", slug, ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error retrieving category safety rule for slug '{}'. Falling back to registry. Error: {}", slug, ex.getMessage());
        }

        return CategorySafetyRule.getBySlug(slug);
    }

    public CategorySafetyRule getRuleByCategoryId(UUID categoryId) {
        if (categoryId == null) {
            return CategorySafetyRule.DEFAULT_RULE;
        }

        try {
            var entityOpt = repository.findByCategoryId(categoryId)
                    .or(() -> repository.findById(categoryId));
            if (entityOpt.isPresent()) {
                return mapToDomain(entityOpt.get());
            }
        } catch (DataAccessException ex) {
            log.error("Database error retrieving category safety rule for categoryId '{}'. Error: {}", categoryId, ex.getMessage());
        }

        throw new CategorySafetyRuleNotFoundException("Không tìm thấy quy chuẩn an toàn cho danh mục với ID: " + categoryId);
    }

    @Transactional
    @CacheEvict(value = "categorySafetyRules", allEntries = true)
    public CategorySafetyRule updateRule(UUID categoryId, UpdateCategorySafetyRuleRequest request) {
        if (categoryId == null) {
            throw new CategorySafetyRuleNotFoundException("ID danh mục không được để trống");
        }

        CategorySafetyRuleJpaEntity entity = repository.findByCategoryId(categoryId)
                .or(() -> repository.findById(categoryId))
                .orElseThrow(() -> new CategorySafetyRuleNotFoundException("Không tìm thấy quy chuẩn an toàn cho danh mục với ID: " + categoryId));

        validateThresholds(entity, request);

        if (request.getCautionWaveHeightM() != null) {
            entity.setCautionWaveHeightM(request.getCautionWaveHeightM());
        }
        if (request.getMaxWaveHeightM() != null) {
            entity.setMaxWaveHeightM(request.getMaxWaveHeightM());
        }
        if (request.getCautionWindSpeedKmh() != null) {
            entity.setCautionWindSpeedKmh(request.getCautionWindSpeedKmh());
        }
        if (request.getMaxWindSpeedKmh() != null) {
            entity.setMaxWindSpeedKmh(request.getMaxWindSpeedKmh());
        }
        if (request.getMaxWindGustKmh() != null) {
            entity.setMaxWindGustKmh(request.getMaxWindGustKmh());
        }
        if (request.getMaxOceanCurrentMs() != null) {
            entity.setMaxOceanCurrentMs(request.getMaxOceanCurrentMs());
        }
        if (request.getMinVisibilityM() != null) {
            entity.setMinVisibilityM(request.getMinVisibilityM());
        }
        if (request.getFatalThunderstormCodes() != null) {
            entity.setFatalThunderstormCodes(request.getFatalThunderstormCodes());
        }

        CategorySafetyRuleJpaEntity saved = repository.save(entity);
        log.info("Successfully updated safety rules for category '{}' (id: {}). Evicted cache.", saved.getCategorySlug(), categoryId);

        return mapToDomain(saved);
    }

    public void validateThresholds(CategorySafetyRuleJpaEntity entity, UpdateCategorySafetyRuleRequest request) {
        if (request == null) {
            return;
        }

        // 1. Kiểm tra không âm
        if ((request.getCautionWaveHeightM() != null && request.getCautionWaveHeightM() < 0) ||
            (request.getMaxWaveHeightM() != null && request.getMaxWaveHeightM() < 0) ||
            (request.getCautionWindSpeedKmh() != null && request.getCautionWindSpeedKmh() < 0) ||
            (request.getMaxWindSpeedKmh() != null && request.getMaxWindSpeedKmh() < 0) ||
            (request.getMaxWindGustKmh() != null && request.getMaxWindGustKmh() < 0) ||
            (request.getMaxOceanCurrentMs() != null && request.getMaxOceanCurrentMs() < 0) ||
            (request.getMinVisibilityM() != null && request.getMinVisibilityM() < 0)) {
            throw new InvalidSafetyRuleThresholdException("Thông số ngưỡng an toàn không được là số âm");
        }

        // 2. Kiểm tra ngưỡng sóng: caution <= max
        Double mergedCautionWave = request.getCautionWaveHeightM() != null ? request.getCautionWaveHeightM() : entity.getCautionWaveHeightM();
        Double mergedMaxWave = request.getMaxWaveHeightM() != null ? request.getMaxWaveHeightM() : entity.getMaxWaveHeightM();
        if (mergedCautionWave != null && mergedMaxWave != null && mergedCautionWave > mergedMaxWave) {
            throw new InvalidSafetyRuleThresholdException(String.format(
                    "Ngưỡng sóng vàng (%.2fm) không được vượt quá ngưỡng sóng đỏ (%.2fm)", mergedCautionWave, mergedMaxWave));
        }

        // 3. Kiểm tra ngưỡng gió: caution <= max
        Double mergedCautionWind = request.getCautionWindSpeedKmh() != null ? request.getCautionWindSpeedKmh() : entity.getCautionWindSpeedKmh();
        Double mergedMaxWind = request.getMaxWindSpeedKmh() != null ? request.getMaxWindSpeedKmh() : entity.getMaxWindSpeedKmh();
        if (mergedCautionWind != null && mergedMaxWind != null && mergedCautionWind > mergedMaxWind) {
            throw new InvalidSafetyRuleThresholdException(String.format(
                    "Ngưỡng gió duy trì vàng (%.2f km/h) không được vượt quá ngưỡng gió đỏ (%.2f km/h)", mergedCautionWind, mergedMaxWind));
        }
    }

    public CategorySafetyRule mapToDomain(CategorySafetyRuleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return CategorySafetyRule.builder()
                .id(entity.getId())
                .categoryId(entity.getCategoryId())
                .categorySlug(entity.getCategorySlug())
                .categoryName(entity.getCategoryName())
                .cautionWaveHeightM(entity.getCautionWaveHeightM())
                .maxWaveHeightM(entity.getMaxWaveHeightM())
                .cautionWindSpeedKmh(entity.getCautionWindSpeedKmh())
                .maxWindSpeedKmh(entity.getMaxWindSpeedKmh())
                .maxWindGustKmh(entity.getMaxWindGustKmh())
                .maxOceanCurrentMs(entity.getMaxOceanCurrentMs())
                .minVisibilityM(entity.getMinVisibilityM())
                .fatalThunderstormCodes(entity.getFatalThunderstormCodes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public CategorySafetyRuleResponse mapToResponse(CategorySafetyRule rule) {
        if (rule == null) {
            return null;
        }
        return CategorySafetyRuleResponse.builder()
                .id(rule.getId())
                .categoryId(rule.getCategoryId())
                .categorySlug(rule.getCategorySlug())
                .categoryName(rule.getCategoryName())
                .cautionWaveHeightM(rule.getCautionWaveHeightM())
                .maxWaveHeightM(rule.getMaxWaveHeightM())
                .cautionWindSpeedKmh(rule.getCautionWindSpeedKmh())
                .maxWindSpeedKmh(rule.getMaxWindSpeedKmh())
                .maxWindGustKmh(rule.getMaxWindGustKmh())
                .maxOceanCurrentMs(rule.getMaxOceanCurrentMs())
                .minVisibilityM(rule.getMinVisibilityM())
                .fatalThunderstormCodes(rule.getFatalThunderstormCodes())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
