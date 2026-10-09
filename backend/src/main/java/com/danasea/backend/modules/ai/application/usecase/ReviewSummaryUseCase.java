package com.danasea.backend.modules.ai.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.danasea.backend.modules.ai.application.port.DecisionModelPort;
import com.danasea.backend.modules.ai.application.port.ReviewHighlightPort;
import com.danasea.backend.modules.ai.application.port.TravelDataPort;
import com.danasea.backend.modules.ai.domain.models.DecisionResult;
import com.danasea.backend.modules.ai.domain.models.DecisionTask;
import com.danasea.backend.modules.operation.application.api.AiReviewReadApi.ReviewEvidence;
import com.danasea.backend.shared.i18n.SupportedLanguage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewSummaryUseCase {
    public record AnalyzedReview(ReviewEvidence source, DecisionResult analysis) {}
    public record Result(UUID serviceId, String status, long totalPublicReviews, int sampledReviews,
                         BigDecimal sampledAverageRating, long positiveRatingCount, long negativeRatingCount,
                         List<ReviewEvidence> highlights, List<ReviewEvidence> positiveExamples, List<ReviewEvidence> negativeExamples, List<AnalyzedReview> analyses,
                         String selectionMethod, boolean exhaustive, String claimScope) {}
    private final TravelDataPort data;
    private final DecisionModelPort decisions;
    private final ReviewHighlightPort highlights;

    public Result execute(UUID serviceId, SupportedLanguage language) {
        data.metadata(serviceId, language);
        List<ReviewEvidence> sources = data.reviews(serviceId, 50);
        long count = data.reviewCount(serviceId);
        if (sources.isEmpty()) return new Result(serviceId, "NO_REVIEWS", count, 0, null, 0, 0,
                List.of(), List.of(), List.of(), List.of(), "NONE", count == 0, "PUBLIC_UNFLAGGED_REVIEW_SAMPLE_ONLY");
        List<UUID> selected = highlights.select(sources, language);
        String method = selected.isEmpty() ? "EXTRACTIVE_BASELINE" : "GROQ_SELECTED_VERIFIED_SOURCE_IDS";
        if (selected.isEmpty()) {
            selected = sources.stream().sorted(Comparator.comparingInt(ReviewEvidence::rating))
                    .limit(3).map(ReviewEvidence::id).toList();
            var combined = new LinkedHashSet<>(selected);
            sources.stream().sorted(Comparator.comparingInt(ReviewEvidence::rating).reversed())
                    .limit(3).map(ReviewEvidence::id).forEach(combined::add);
            selected = List.copyOf(combined);
        }
        List<AnalyzedReview> analyses = new ArrayList<>();
        for (ReviewEvidence source : sources.stream().limit(20).toList()) {
            DecisionResult analysis = source.comment() == null || source.comment().isBlank()
                    ? DecisionResult.unavailable(DecisionTask.REVIEW, "EMPTY_REVIEW")
                    : decisions.decide(DecisionTask.REVIEW, Map.of("review", source.comment()));
            analyses.add(new AnalyzedReview(source, analysis));
        }
        List<UUID> selection = selected;
        BigDecimal average = BigDecimal.valueOf(sources.stream().mapToInt(ReviewEvidence::rating).sum())
                .divide(BigDecimal.valueOf(sources.size()), 2, RoundingMode.HALF_UP);
        return new Result(serviceId, "AVAILABLE", count, sources.size(), average,
                sources.stream().filter(review -> review.rating() >= 4).count(),
                sources.stream().filter(review -> review.rating() <= 2).count(),
                sources.stream().filter(review -> selection.contains(review.id())).toList(),
                sources.stream().filter(review -> review.rating() >= 4).limit(3).toList(),
                sources.stream().filter(review -> review.rating() <= 2).limit(3).toList(), List.copyOf(analyses),
                method, count == sources.size(), "PUBLIC_UNFLAGGED_REVIEW_SAMPLE_ONLY; MODEL_ASPECTS_ARE_SUGGESTIONS");
    }
}
