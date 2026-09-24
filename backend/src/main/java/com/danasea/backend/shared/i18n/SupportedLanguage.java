package com.danasea.backend.shared.i18n;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum SupportedLanguage {
    VI("vi"),
    EN("en");

    public static final SupportedLanguage DEFAULT = VI;

    private final String code;
    private final Locale locale;

    SupportedLanguage(String code) {
        this.code = code;
        this.locale = Locale.forLanguageTag(code);
    }

    public String code() {
        return code;
    }

    public Locale locale() {
        return locale;
    }

    public static Optional<SupportedLanguage> fromTag(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String language = Locale.forLanguageTag(value.trim().replace('_', '-')).getLanguage();
        for (SupportedLanguage supported : values()) {
            if (supported.code.equalsIgnoreCase(language)) {
                return Optional.of(supported);
            }
        }
        return Optional.empty();
    }

    public static Optional<SupportedLanguage> fromAcceptLanguage(String header) {
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        try {
            return Locale.LanguageRange.parse(header).stream()
                    .filter(range -> range.getWeight() > 0 && !"*".equals(range.getRange()))
                    .sorted(Comparator.comparingDouble(Locale.LanguageRange::getWeight).reversed())
                    .map(Locale.LanguageRange::getRange)
                    .map(SupportedLanguage::fromTag)
                    .flatMap(Optional::stream)
                    .findFirst();
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static List<Locale> locales() {
        return List.of(VI.locale, EN.locale);
    }
}
