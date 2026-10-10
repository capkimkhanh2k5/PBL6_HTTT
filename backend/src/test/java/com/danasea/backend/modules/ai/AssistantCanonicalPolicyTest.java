package com.danasea.backend.modules.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.danasea.backend.modules.ai.application.port.TravelPolicyPort;
import com.danasea.backend.modules.ai.application.tool.GetPolicyTool;
import com.danasea.backend.modules.order.application.api.AiPolicyReadApi;
import com.danasea.backend.modules.systemconfig.infrastructure.persistence.repositories.JpaSystemConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class AssistantCanonicalPolicyTest {
    @Test void runtimeRefundPolicyUsesCanonicalRulesAndDoesNotPromiseAutomaticFreeReschedule() throws Exception {
        var configs = mock(JpaSystemConfigRepository.class);
        var rules = mock(TravelPolicyPort.class);
        when(rules.current()).thenReturn(new AiPolicyReadApi.Snapshot("PLATFORM",
                List.of(new AiPolicyReadApi.RefundTier("MORE_THAN_48_HOURS", new BigDecimal("100"))),
                new BigDecimal("100"), "RefundPolicyEngine", "REVALIDATE_WITH_OWNED_ORDER_PREVIEW"));
        var mapper = new ObjectMapper();
        var tool = new GetPolicyTool(configs, mapper);
        tool.setCanonicalPolicy(rules);
        var result = mapper.readTree(tool.execute("{\"policy_type\":\"WEATHER_CANCELLATION\"}"));
        assertThat(result.path("policy").path("source").asText()).isEqualTo("RefundPolicyEngine");
        assertThat(result.path("rescheduleSupported").asBoolean()).isFalse();
        assertThat(result.path("changeRequestRequiresHumanApproval").asBoolean()).isTrue();
        assertThat(result.path("orderSpecificEligibilityChecked").asBoolean()).isFalse();
        verifyNoInteractions(configs);
    }
}
