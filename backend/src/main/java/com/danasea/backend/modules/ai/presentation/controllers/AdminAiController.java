package com.danasea.backend.modules.ai.presentation.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.ai.application.port.AssessmentCaseStorePort;
import com.danasea.backend.modules.ai.application.usecase.AnalyzeTransactionRiskUseCase;
import com.danasea.backend.modules.ai.domain.models.AssessmentCase;
import com.danasea.backend.security.infrastructure.SecurityUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAiController {
    public record RiskRequest(UUID orderId, String complaint) {}
    public record ResolutionRequest(String resolution, String note) {}
    private final AnalyzeTransactionRiskUseCase risks;
    private final AssessmentCaseStorePort cases;

    @PostMapping("/risk-cases")
    public AssessmentCase risk(@RequestBody RiskRequest request) {
        return risks.execute(SecurityUtils.getCurrentUserId().orElseThrow(), request.orderId(), request.complaint());
    }

    @GetMapping("/assessment-cases")
    public List<AssessmentCase> cases(@RequestParam(defaultValue = "NEEDS_REVIEW") String status,
                                      @RequestParam(defaultValue = "50") int limit) { return cases.list(status, limit); }

    @PostMapping("/assessment-cases/{id}/resolve")
    public AssessmentCase resolve(@PathVariable UUID id, @RequestBody ResolutionRequest request) {
        return cases.resolve(id, SecurityUtils.getCurrentUserId().orElseThrow(), request.resolution(), request.note());
    }
}
