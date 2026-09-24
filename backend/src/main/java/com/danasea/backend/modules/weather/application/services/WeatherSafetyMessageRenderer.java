package com.danasea.backend.modules.weather.application.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine.SafetyEvaluationResult;
import com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine.SafetyFinding;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.i18n.SupportedLanguage;

@Service
public class WeatherSafetyMessageRenderer {
    private final LocalizedMessageService messages;

    public WeatherSafetyMessageRenderer() {
        this(LocalizedMessageService.standalone());
    }

    @Autowired
    public WeatherSafetyMessageRenderer(LocalizedMessageService messages) {
        this.messages = messages;
    }

    public String render(SafetyEvaluationResult result, SupportedLanguage language) {
        if (result == null) {
            return messages.get("weather.safety.safe", language);
        }
        List<SafetyFinding> findings = result.getFindings();
        if (findings == null || findings.isEmpty()) {
            return result.getWarningMessage();
        }
        String details = findings.stream()
                .map(finding -> messages.get("weather.finding." + finding.code(), language, finding.args()))
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
        String level = result.getAlertLevel() == null ? "green" : result.getAlertLevel().toLowerCase();
        return messages.get("weather.summary." + level, language, details);
    }

    public List<String> renderDetails(SafetyEvaluationResult result, SupportedLanguage language) {
        if (result == null || result.getFindings() == null || result.getFindings().isEmpty()) {
            return result == null || result.getDetails() == null ? List.of() : result.getDetails();
        }
        return result.getFindings().stream()
                .map(finding -> messages.get("weather.finding." + finding.code(), language, finding.args()))
                .toList();
    }
}
