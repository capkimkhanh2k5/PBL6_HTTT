package com.danasea.backend.modules.weather.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaSafetyRuleEvaluationRepository extends JpaRepository<SafetyRuleEvaluationJpaEntity, UUID> {
    List<SafetyRuleEvaluationJpaEntity> findByIsSafeFalse();
    List<SafetyRuleEvaluationJpaEntity> findByStatus(String status);
    List<SafetyRuleEvaluationJpaEntity> findByStatusIn(List<String> statuses);
    Optional<SafetyRuleEvaluationJpaEntity> findTopBySlotIdOrderByEvaluatedAtDesc(UUID slotId);
    Optional<SafetyRuleEvaluationJpaEntity> findTopBySlotIdAndStatusInOrderByEvaluatedAtDesc(UUID slotId, List<String> statuses);
    List<SafetyRuleEvaluationJpaEntity> findBySlotId(UUID slotId);
}
