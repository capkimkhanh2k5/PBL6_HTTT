package com.danasea.backend.shared.i18n;

public class UnsupportedLanguageException extends LocalizedException {
    public UnsupportedLanguageException(String language) {
        super("UNSUPPORTED_LANGUAGE", "error.unsupported_language", language);
    }
}
