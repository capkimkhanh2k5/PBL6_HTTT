package com.danasea.backend.modules.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.danasea.backend.modules.ai.application.ports.AiExecutionBudget;

class AiExecutionBudgetTest {
    @Test void budgetBoundsModelFanoutAndDoesNotLeakAcrossRequests() {
        try (var ignored = AiExecutionBudget.open(Duration.ofSeconds(2), 2)) {
            assertThat(AiExecutionBudget.permitDecision(Duration.ZERO)).isTrue();
            assertThat(AiExecutionBudget.permitDecision(Duration.ZERO)).isTrue();
            assertThat(AiExecutionBudget.permitDecision(Duration.ZERO)).isFalse();
        }
        assertThat(AiExecutionBudget.permitDecision(Duration.ZERO)).isTrue();
    }
    @Test void insufficientTimeUsesFallbackBeforeStartingTransport() {
        try (var ignored = AiExecutionBudget.open(Duration.ofMillis(1), 6)) {
            assertThat(AiExecutionBudget.permitDecision(Duration.ofSeconds(3))).isFalse();
        }
    }
}
