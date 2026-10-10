package com.danasea.backend.modules.ai.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_assessment_cases")
@Getter
@Setter
public class AiAssessmentCaseJpaEntity extends BaseJpaEntity {
    @Column(nullable = false, length = 40) private String kind;
    private UUID sourceId;
    @Column(nullable = false) private UUID requestedBy;
    @Column(nullable = false, length = 40) private String status;
    @Column(nullable = false, columnDefinition = "TEXT") private String evidenceJson;
    @Column(nullable = false, columnDefinition = "TEXT") private String decisionJson;
    private UUID resolvedBy;
    @Column(length = 1000) private String resolutionNote;
    @Version private Long version;
}
