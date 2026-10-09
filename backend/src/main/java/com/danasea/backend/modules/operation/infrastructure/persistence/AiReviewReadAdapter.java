package com.danasea.backend.modules.operation.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.operation.application.api.AiReviewReadApi;
import com.danasea.backend.modules.operation.infrastructure.persistence.entities.ReviewJpaEntity;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiReviewReadAdapter implements AiReviewReadApi {
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewEvidence> findPublicByService(UUID serviceId, int limit) {
        return entityManager.createQuery("""
                select r from ReviewJpaEntity r where r.serviceId = :serviceId
                and (r.isFlagged = false or r.isFlagged is null) and r.rating between 1 and 5
                order by r.createdAt desc, r.id
                """, ReviewJpaEntity.class).setParameter("serviceId", serviceId)
                .setMaxResults(Math.min(50, Math.max(1, limit))).getResultList().stream()
                .map(review -> new ReviewEvidence(review.getId(), review.getServiceId(), review.getRating(),
                        review.getComment(), review.getCreatedAt())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPublicByService(UUID serviceId) {
        return entityManager.createQuery("""
                select count(r) from ReviewJpaEntity r where r.serviceId = :serviceId
                and (r.isFlagged = false or r.isFlagged is null) and r.rating between 1 and 5
                """, Long.class).setParameter("serviceId", serviceId).getSingleResult();
    }
}
