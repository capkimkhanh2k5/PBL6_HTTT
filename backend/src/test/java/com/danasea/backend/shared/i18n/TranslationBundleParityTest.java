package com.danasea.backend.shared.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TranslationBundleParityTest {

    private static final Set<String> NAMESPACES = Set.of(
            "common", "validation", "auth", "booking",
            "weather", "notification", "ai", "policy");

    @Test
    void englishAndVietnameseBundlesHaveIdenticalKeys() throws Exception {
        for (String namespace : NAMESPACES) {
            Properties english = load(namespace + "_en.properties");
            Properties vietnamese = load(namespace + "_vi.properties");
            assertEquals(vietnamese.stringPropertyNames(), english.stringPropertyNames(),
                    () -> "Translation key mismatch in namespace " + namespace);
        }
    }

    private Properties load(String fileName) throws Exception {
        String path = "i18n/" + fileName;
        InputStream input = getClass().getClassLoader().getResourceAsStream(path);
        if (input == null) {
            throw new IllegalStateException("Missing translation bundle " + path);
        }
        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}
