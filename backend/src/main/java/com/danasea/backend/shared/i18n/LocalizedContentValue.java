package com.danasea.backend.shared.i18n;

public record LocalizedContentValue(String vietnamese, String english) implements LocalizedArgument {
    @Override
    public Object resolve(SupportedLanguage language) {
        if (language == SupportedLanguage.EN && english != null && !english.isBlank()) {
            return english;
        }
        return vietnamese;
    }
}
