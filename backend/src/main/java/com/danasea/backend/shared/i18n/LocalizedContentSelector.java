package com.danasea.backend.shared.i18n;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class LocalizedContentSelector {

    private static final Logger log = LoggerFactory.getLogger(LocalizedContentSelector.class);

    private final MeterRegistry meterRegistry;

    public LocalizedContentSelector(ObjectProvider<MeterRegistry> meterRegistry) {
        this.meterRegistry = meterRegistry != null ? meterRegistry.getIfAvailable() : null;
    }

    public LocalizedContentSelector() {
        this.meterRegistry = null;
    }

    public String select(String vietnamese, String english) {
        return select(vietnamese, english, languageFromContext());
    }

    public String select(String vietnamese, String english, SupportedLanguage language) {
        if (language == SupportedLanguage.EN) {
            if (english != null && !english.isBlank()) {
                return english;
            }
            recordEnglishFallback();
        }
        return vietnamese;
    }

    private SupportedLanguage languageFromContext() {
        Locale locale = LocaleContextHolder.getLocale();
        return SupportedLanguage.fromTag(locale.toLanguageTag()).orElse(SupportedLanguage.VI);
    }

    public record LocalizedSelection(
            String content,
            String language,
            boolean fallbackUsed
    ) {}

    public LocalizedSelection selectDetailed(String vietnamese, String english) {
        return selectDetailed(vietnamese, english, languageFromContext());
    }

    public LocalizedSelection selectDetailed(String vietnamese, String english, SupportedLanguage language) {
        if (language == SupportedLanguage.EN) {
            if (english != null && !english.isBlank()) {
                return new LocalizedSelection(english, "EN", false);
            }
            if (vietnamese != null && !vietnamese.isBlank()) {
                recordEnglishFallback();
                return new LocalizedSelection(vietnamese, "VI", true);
            }
            return new LocalizedSelection(null, "EN", false);
        }
        if (vietnamese != null && !vietnamese.isBlank()) {
            return new LocalizedSelection(vietnamese, "VI", false);
        }
        if (english != null && !english.isBlank()) {
            return new LocalizedSelection(english, "EN", true);
        }
        return new LocalizedSelection(null, "VI", false);
    }

    private void recordEnglishFallback() {
        log.warn("Missing English localized content; falling back to Vietnamese");
        if (meterRegistry != null) {
            meterRegistry.counter("i18n_missing_translation_total", "source", "database").increment();
        }
    }
}
