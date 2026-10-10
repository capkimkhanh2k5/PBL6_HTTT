package com.danasea.backend.modules.operation.application.usecases;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.operation.domain.exceptions.ReviewNotFoundException;
import com.danasea.backend.modules.operation.domain.exceptions.ReviewPeriodExpiredException;
import com.danasea.backend.modules.operation.domain.exceptions.UnauthorizedReviewAccessException;
import com.danasea.backend.modules.operation.domain.services.ReviewRatingService;
import com.danasea.backend.modules.operation.infrastructure.persistence.entities.ReviewJpaEntity;
import com.danasea.backend.modules.operation.infrastructure.persistence.mappers.ReviewMapper;
import com.danasea.backend.modules.operation.infrastructure.persistence.repositories.JpaReviewRepository;
import com.danasea.backend.modules.operation.presentation.dtos.ReviewResponse;
import com.danasea.backend.modules.operation.presentation.dtos.UpdateReviewRequest;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateReviewUseCase {

    private final JpaReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final ReviewRatingService reviewRatingService;

    @Transactional
    public ReviewResponse execute(UUID reviewId, UpdateReviewRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedReviewAccessException("User is not authenticated"));
        return execute(reviewId, request, currentUserId);
    }

    @Transactional
    public ReviewResponse execute(UUID reviewId, UpdateReviewRequest request, UUID customerId) {
        log.info("Updating review: {} by customer: {}", reviewId, customerId);

        ReviewJpaEntity review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        if (!review.getCustomerId().equals(customerId)) {
            throw new UnauthorizedReviewAccessException("User is not the author of this review");
        }

        // Chính sách: Cho phép sửa đánh giá trong vòng 7 ngày sau khi gửi
        if (review.getCreatedAt() != null && review.getCreatedAt().plusDays(7).isBefore(OffsetDateTime.now())) {
            throw new ReviewPeriodExpiredException("Reviews can only be edited within 7 days of submission");
        }

        review.setRating(request.rating());
        review.setComment(request.comment());
        if (request.images() != null) {
            review.setImages(reviewMapper.serializeImages(request.images()));
        }

        ReviewJpaEntity saved = reviewRepository.save(review);

        // Tính toán lại điểm số service và vendor
        reviewRatingService.recalculateRatings(saved.getServiceId(), saved.getVendorId());

        return reviewMapper.toResponse(saved);
    }

    @Transactional
    public ReviewResponse executeBySubOrder(UUID subOrderId, UpdateReviewRequest request, UUID customerId) {
        UUID reviewId = reviewRepository.findReviewIdBySubOrderId(subOrderId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found for sub-order: " + subOrderId));
        return execute(reviewId, request, customerId);
    }
}
