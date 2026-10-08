package com.danasea.backend.modules.operation.domain.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import com.danasea.backend.modules.operation.infrastructure.persistence.repositories.JpaReviewRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.vendor.domain.models.BadgeTier;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewRatingService {

    private final JpaReviewRepository reviewRepository;
    private final JpaServiceRepository serviceRepository;
    private final JpaVendorRepository vendorRepository;

    @Transactional
    public void recalculateRatings(UUID serviceId, UUID vendorId) {
        if (serviceId != null) {
            recalculateServiceRating(serviceId);
        }
        if (vendorId != null) {
            recalculateVendorRating(vendorId);
        }
    }

    @Transactional
    public void recalculateServiceRating(UUID serviceId) {
        ServiceJpaEntity serviceEntity = serviceRepository.findById(serviceId).orElse(null);
        if (serviceEntity == null) {
            log.warn("Service not found for rating recalculation: {}", serviceId);
            return;
        }

        long count = reviewRepository.countByServiceIdAndIsVisibleTrue(serviceId);
        Double avg = reviewRepository.getAvgRatingByServiceId(serviceId);
        BigDecimal avgRating = (avg != null && count > 0)
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        serviceEntity.setAvgRating(avgRating);
        serviceEntity.setRatingCount((int) count);
        serviceRepository.save(serviceEntity);
        log.info("Recalculated Service [{}] - avgRating: {}, ratingCount: {}", serviceId, avgRating, count);
    }

    @Transactional
    public void recalculateVendorRating(UUID vendorId) {
        VendorJpaEntity vendorEntity = vendorRepository.findById(vendorId).orElse(null);
        if (vendorEntity == null) {
            log.warn("Vendor not found for rating recalculation: {}", vendorId);
            return;
        }

        long count = reviewRepository.countByVendorIdAndIsVisibleTrue(vendorId);
        Double avg = reviewRepository.getAvgRatingByVendorId(vendorId);
        BigDecimal ratingAvg = (avg != null && count > 0)
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BadgeTier badgeTier;
        if (count >= 5 && ratingAvg.compareTo(new BigDecimal("4.0")) >= 0) {
            badgeTier = BadgeTier.TOP_RATED;
        } else if (count >= 1) {
            badgeTier = BadgeTier.VERIFIED;
        } else {
            badgeTier = BadgeTier.NONE;
        }

        vendorEntity.setRatingAvg(ratingAvg);
        vendorEntity.setRatingCount((int) count);
        vendorEntity.setBadgeTier(badgeTier);
        vendorRepository.save(vendorEntity);
        log.info("Recalculated Vendor [{}] - ratingAvg: {}, ratingCount: {}, badgeTier: {}", vendorId, ratingAvg, count, badgeTier);
    }
}
