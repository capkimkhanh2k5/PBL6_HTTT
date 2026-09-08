package com.danasea.backend.modules.weather.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.weather.infrastructure.persistence.entities.SafetyRuleEvaluationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaSafetyRuleEvaluationRepository extends JpaRepository<SafetyRuleEvaluationJpaEntity, UUID> {
}
