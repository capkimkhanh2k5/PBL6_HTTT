package com.danasea.backend.modules.operation.application.usecases;

import java.util.UUID;

import com.danasea.backend.modules.operation.domain.exceptions.ReviewNotFoundException;
import com.danasea.backend.modules.operation.domain.services.ReviewRatingService;
import com.danasea.backend.modules.operation.infrastructure.persistence.entities.ReviewJpaEntity;
import com.danasea.backend.modules.operation.infrastructure.persistence.mappers.ReviewMapper;
import com.danasea.backend.modules.operation.infrastructure.persistence.repositories.JpaReviewRepository;
import com.danasea.backend.modules.operation.presentation.dtos.ReviewResponse;
import com.danasea.backend.modules.operation.presentation.dtos.UpdateReviewVisibilityRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateReviewVisibilityUseCase {

    private final JpaReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final ReviewRatingService reviewRatingService;

    @Transactional
    public ReviewResponse execute(UUID reviewId, UpdateReviewVisibilityRequest request) {
        log.info("Updating visibility for review: {} to: {}", reviewId, request.isVisible());

        ReviewJpaEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        boolean wasVisible = Boolean.TRUE.equals(review.getIsVisible());
        boolean newVisible = Boolean.TRUE.equals(request.isVisible());

        review.setIsVisible(newVisible);
        if (request.note() != null) {
            review.setFlagReason(request.note());
        }

        ReviewJpaEntity saved = reviewRepository.save(review);

        // Nếu trạng thái hiển thị thay đổi, lập tức tính toán lại điểm service và vendor!
        if (wasVisible != newVisible) {
            log.info("Visibility changed for review [{}], recalculating ratings for service [{}] and vendor [{}]",
                    reviewId, saved.getServiceId(), saved.getVendorId());
            reviewRatingService.recalculateRatings(saved.getServiceId(), saved.getVendorId());
        }

        return reviewMapper.toResponse(saved);
    }
}
