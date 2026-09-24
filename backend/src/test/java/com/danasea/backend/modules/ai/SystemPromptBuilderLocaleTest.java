package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.domain.services.SystemPromptBuilder;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemPromptBuilderLocaleTest {

    private final SystemPromptBuilder promptBuilder = new SystemPromptBuilder();

    @Test
    void promptPreservesSafetyRulesAndAddsEnglishLanguageRule() {
        String prompt = promptBuilder.buildBasePrompt(SupportedLanguage.EN);

        assertTrue(prompt.contains("MUST ALWAYS call the get_policy"));
        assertTrue(prompt.contains("Answer every user-facing response in English."));
    }

    @Test
    void promptPreservesSafetyRulesAndAddsVietnameseLanguageRule() {
        String prompt = promptBuilder.buildBasePrompt(SupportedLanguage.VI);

        assertTrue(prompt.contains("MUST ALWAYS call the get_policy"));
        assertTrue(prompt.contains("Luôn trả lời mọi nội dung dành cho người dùng bằng tiếng Việt."));
    }
}
