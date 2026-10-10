package com.danasea.backend.modules.ai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.danasea.backend.modules.ai.application.tools.CustomerFeatureTool;
import com.danasea.backend.modules.ai.application.tools.ToolExecutionContext;
import com.danasea.backend.modules.ai.application.usecases.CustomerSupportUseCase;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import com.fasterxml.jackson.databind.ObjectMapper;

class CustomerFeatureToolIdentityTest {
    @Test void privateToolsRequireServerActorAndIgnoreUserIdInModelArguments() throws Exception {
        var support = mock(CustomerSupportUseCase.class);
        var tool = new CustomerFeatureTool("ai_customer_support", null, null, null, null, null, support, new ObjectMapper());
        UUID real = UUID.randomUUID(), spoofed = UUID.randomUUID(), order = UUID.randomUUID();
        String args = "{\"userId\":\"" + spoofed + "\",\"orderId\":\"" + order + "\",\"message\":\"Refund?\"}";
        assertThat(new ObjectMapper().readTree(tool.execute(args)).path("error").asText()).isEqualTo("AUTHENTICATION_REQUIRED");
        verifyNoInteractions(support);
        tool.execute(args, new ToolExecutionContext(SupportedLanguage.VI, UUID.randomUUID(), real));
        verify(support).execute(real, order, "Refund?", false);
        verify(support, never()).execute(eq(spoofed), any(), any(), anyBoolean());
    }
}
