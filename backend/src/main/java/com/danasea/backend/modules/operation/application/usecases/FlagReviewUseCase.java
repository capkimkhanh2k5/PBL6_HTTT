package com.danasea.backend.modules.operation.application.usecases;

import java.util.UUID;

import com.danasea.backend.modules.operation.domain.exceptions.ReviewNotFoundException;
import com.danasea.backend.modules.operation.infrastructure.persistence.entities.ReviewJpaEntity;
import com.danasea.backend.modules.operation.infrastructure.persistence.repositories.JpaReviewRepository;
import com.danasea.backend.modules.operation.presentation.dtos.FlagReviewRequest;
import com.danasea.backend.modules.operation.presentation.dtos.FlagReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlagReviewUseCase {

    private final JpaReviewRepository reviewRepository;

    @Transactional
    public FlagReviewResponse execute(UUID reviewId, FlagReviewRequest request) {
        log.info("Flagging review: {} with reason: {}", reviewId, request.reason());

        ReviewJpaEntity review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + reviewId));

        if (!Boolean.TRUE.equals(review.getIsVisible())) {
            throw new ReviewNotFoundException("Review not found");
        }
        review.setIsFlagged(true);
        review.setFlagReason(request.reason());

        ReviewJpaEntity saved = reviewRepository.save(review);
        return new FlagReviewResponse(saved.getId(), true);
    }
}
