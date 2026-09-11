package com.danasea.backend.modules.service.infrastructure.persistence.specifications;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class ServiceSpecifications {

    private ServiceSpecifications() {
    }

    public static Specification<ServiceJpaEntity> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("status"), ServiceStatus.PUBLISHED);
    }

    public static Specification<ServiceJpaEntity> hasCategory(UUID categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("categoryId"), categoryId);
        };
    }

    public static Specification<ServiceJpaEntity> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
            Predicate nameEnMatch = cb.like(cb.lower(root.get("nameEn")), pattern);
            Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
            return cb.or(nameMatch, nameEnMatch, descMatch);
        };
    }

    public static Specification<ServiceJpaEntity> inPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<ServiceJpaEntity> withinLocation(BigDecimal lat, BigDecimal lng, Double radiusKm) {
        return (root, query, cb) -> {
            if (lat == null || lng == null || radiusKm == null || radiusKm <= 0) {
                return cb.conjunction();
            }

            double latVal = lat.doubleValue();
            double lngVal = lng.doubleValue();
            double deltaLat = radiusKm / 111.0;
            double latRad = Math.toRadians(latVal);
            double cosLat = Math.cos(latRad);
            double deltaLng = (Math.abs(cosLat) > 0.0001) ? (radiusKm / (111.0 * Math.abs(cosLat))) : (radiusKm / 111.0);

            BigDecimal minLat = BigDecimal.valueOf(latVal - deltaLat);
            BigDecimal maxLat = BigDecimal.valueOf(latVal + deltaLat);
            BigDecimal minLng = BigDecimal.valueOf(lngVal - deltaLng);
            BigDecimal maxLng = BigDecimal.valueOf(lngVal + deltaLng);

            Predicate notNull = cb.and(root.get("latitude").isNotNull(), root.get("longitude").isNotNull());
            Predicate latBetween = cb.between(root.get("latitude"), minLat, maxLat);
            Predicate lngBetween = cb.between(root.get("longitude"), minLng, maxLng);

            return cb.and(notNull, latBetween, lngBetween);
        };
    }

    public static Specification<ServiceJpaEntity> filter(
            UUID categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal lat,
            BigDecimal lng,
            Double radiusKm
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory status = PUBLISHED in all queries
            predicates.add(cb.equal(root.get("status"), ServiceStatus.PUBLISHED));

            // 2. categoryId
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }

            // 3. keyword case-insensitive match on name, nameEn, description
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate nameEnMatch = cb.like(cb.lower(root.get("nameEn")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameMatch, nameEnMatch, descMatch));
            }

            // 4. Price range
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // 5. Location bounding box
            if (lat != null && lng != null && radiusKm != null && radiusKm > 0) {
                double latVal = lat.doubleValue();
                double lngVal = lng.doubleValue();
                double deltaLat = radiusKm / 111.0;
                double latRad = Math.toRadians(latVal);
                double cosLat = Math.cos(latRad);
                double deltaLng = (Math.abs(cosLat) > 0.0001) ? (radiusKm / (111.0 * Math.abs(cosLat))) : (radiusKm / 111.0);

                BigDecimal minLat = BigDecimal.valueOf(latVal - deltaLat);
                BigDecimal maxLat = BigDecimal.valueOf(latVal + deltaLat);
                BigDecimal minLng = BigDecimal.valueOf(lngVal - deltaLng);
                BigDecimal maxLng = BigDecimal.valueOf(lngVal + deltaLng);

                Predicate notNull = cb.and(root.get("latitude").isNotNull(), root.get("longitude").isNotNull());
                Predicate latBetween = cb.between(root.get("latitude"), minLat, maxLat);
                Predicate lngBetween = cb.between(root.get("longitude"), minLng, maxLng);
                predicates.add(cb.and(notNull, latBetween, lngBetween));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
