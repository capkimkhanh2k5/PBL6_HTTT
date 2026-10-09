package com.danasea.backend.modules.weather.application.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiItineraryAlertReader implements AiItineraryAlertReadApi {
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<Signal> activeSignals(Set<UUID> slotIds) {
        if (slotIds.isEmpty()) return List.of();
        List<SafetyRuleEvaluationJpaEntity> evaluations = entityManager.createQuery(
                "select e from SafetyRuleEvaluationJpaEntity e where e.slotId in :slots and not exists "
                + "(select later.id from SafetyRuleEvaluationJpaEntity later where later.slotId=e.slotId and later.evaluatedAt>e.evaluatedAt)",
                SafetyRuleEvaluationJpaEntity.class).setParameter("slots", slotIds).getResultList();
        return evaluations.stream().filter(e -> Boolean.FALSE.equals(e.getIsSafe()) || "RED".equals(e.getAlertLevel()))
                .filter(e -> !"RESOLVED".equals(e.getStatus()) && !"RESOLVED_MANUALLY".equals(e.getStatus()))
                .map(e -> new Signal(e.getSlotId(), "WEATHER:" + e.getId() + ":" + e.getStatus() + ":" + e.getUpdatedAt(),
                        "CANONICAL_WEATHER_ALERT", e.getEvaluatedAt())).toList();
    }
}
