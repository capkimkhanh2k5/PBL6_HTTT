package com.danasea.backend.modules.weather.application.services;

import com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine.SafetyEvaluationResult;
import com.danasea.backend.shared.i18n.LocalizedArgument;
import com.danasea.backend.shared.i18n.SupportedLanguage;

public record LocalizedWeatherEvaluationValue(SafetyEvaluationResult result) implements LocalizedArgument {
    private static final WeatherSafetyMessageRenderer RENDERER = new WeatherSafetyMessageRenderer();

    @Override
    public Object resolve(SupportedLanguage language) {
        return RENDERER.render(result, language);
    }
}
