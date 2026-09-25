package com.danasea.backend.shared.i18n;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class LocalizedContentSelector {

    private static final Logger log = LoggerFactory.getLogger(LocalizedContentSelector.class);

    private final MeterRegistry meterRegistry;

    public LocalizedContentSelector(ObjectProvider<MeterRegistry> meterRegistry) {
        this.meterRegistry = meterRegistry.getIfAvailable();
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

    private void recordEnglishFallback() {
        log.warn("Missing English localized content; falling back to Vietnamese");
        if (meterRegistry != null) {
            meterRegistry.counter("i18n_missing_translation_total", "source", "database").increment();
        }
    }
}
