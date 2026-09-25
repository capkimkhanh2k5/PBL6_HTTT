package com.danasea.backend.shared.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SupportedLanguageTest {

    @Test
    void normalizesRegionalAndUnderscoreTags() {
        assertEquals(SupportedLanguage.EN, SupportedLanguage.fromTag("en-US").orElseThrow());
        assertEquals(SupportedLanguage.VI, SupportedLanguage.fromTag("vi_VN").orElseThrow());
    }

    @Test
    void honorsQualityWeightAndSkipsUnsupportedLanguages() {
        assertEquals(SupportedLanguage.VI,
                SupportedLanguage.fromAcceptLanguage("fr-FR, en;q=0.4, vi-VN;q=0.9").orElseThrow());
    }

    @Test
    void ignoresWildcardAndZeroQuality() {
        assertEquals(SupportedLanguage.VI,
                SupportedLanguage.fromAcceptLanguage("*, en;q=0, vi;q=0.5").orElseThrow());
        assertTrue(SupportedLanguage.fromAcceptLanguage("*, en;q=0").isEmpty());
    }

    @Test
    void rejectsMalformedOrUnsupportedHeaders() {
        assertTrue(SupportedLanguage.fromAcceptLanguage("not valid;q=nope").isEmpty());
        assertTrue(SupportedLanguage.fromAcceptLanguage("fr-FR, de;q=0.8").isEmpty());
    }
}
