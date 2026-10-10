package com.danasea.backend.modules.operation.infrastructure.persistence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.operation.application.api.AiReviewReadApi;
import com.danasea.backend.modules.operation.infrastructure.persistence.entities.ReviewJpaEntity;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiReviewReadAdapter implements AiReviewReadApi {
    private static final String PUBLIC_FILTER = " where r.serviceId = :serviceId and (r.isFlagged = false or r.isFlagged is null) and r.rating between 1 and 5 ";
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewEvidence> findPublicByService(UUID serviceId, int limit) {
        return entityManager.createQuery("select r from ReviewJpaEntity r" + PUBLIC_FILTER + "order by r.createdAt desc, r.id", ReviewJpaEntity.class)
                .setParameter("serviceId", serviceId).setMaxResults(Math.min(50, Math.max(1, limit)))
                .getResultList().stream().map(this::source).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPublicByService(UUID serviceId) {
        return entityManager.createQuery("select count(r) from ReviewJpaEntity r" + PUBLIC_FILTER, Long.class)
                .setParameter("serviceId", serviceId).getSingleResult();
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewSnapshot snapshot(UUID serviceId, int sampleLimit) {
        int limit = Math.min(50, Math.max(1, sampleLimit));
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) distribution.put(rating, 0L);
        MessageDigest digest = digest();
        Map<Integer, List<ReviewEvidence>> bands = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) bands.put(rating, new ArrayList<>());
        long[] totals = {0, 0};
        // Scalar streaming avoids keeping every review entity in the persistence context.
        try (var rows = entityManager.createQuery("select r.id, r.rating, r.comment, r.createdAt, r.updatedAt from ReviewJpaEntity r"
                + PUBLIC_FILTER + "order by r.rating, r.createdAt desc, r.id", Object[].class)
                .setParameter("serviceId", serviceId).setHint("org.hibernate.fetchSize", 100).getResultStream()) {
            rows.forEach(row -> {
                UUID id = (UUID) row[0];
                int rating = ((Number) row[1]).intValue();
                String comment = (String) row[2];
                OffsetDateTime created = (OffsetDateTime) row[3];
                totals[0]++; totals[1] += rating;
                distribution.put(rating, distribution.get(rating) + 1);
                // Length framing prevents source strings from colliding through delimiters.
                for (Object field : row) {
                    byte[] bytes = String.valueOf(field).getBytes(StandardCharsets.UTF_8);
                    digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array()); digest.update(bytes);
                }
                List<ReviewEvidence> band = bands.get(rating);
                if (band.size() < limit) band.add(new ReviewEvidence(id, serviceId, rating, comment, created));
            });
        }
        List<ReviewEvidence> representatives = new ArrayList<>();
        for (int index = 0; index < limit && representatives.size() < limit; index++) {
            for (List<ReviewEvidence> band : bands.values()) {
                if (band.size() > index && representatives.size() < limit) representatives.add(band.get(index));
            }
        }
        representatives.sort(Comparator.comparing(ReviewEvidence::createdAt, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(r -> r.id().toString()));
        BigDecimal average = totals[0] == 0 ? null : BigDecimal.valueOf(totals[1]).divide(BigDecimal.valueOf(totals[0]), 2, RoundingMode.HALF_UP);
        return new ReviewSnapshot(totals[0], average, distribution.get(4) + distribution.get(5), distribution.get(1) + distribution.get(2),
                Map.copyOf(distribution), List.copyOf(representatives), HexFormat.of().formatHex(digest.digest()), OffsetDateTime.now());
    }

    private ReviewEvidence source(ReviewJpaEntity review) {
        return new ReviewEvidence(review.getId(), review.getServiceId(), review.getRating(), review.getComment(), review.getCreatedAt());
    }
    private MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("Review fingerprint is unavailable", exception); }
    }
}
