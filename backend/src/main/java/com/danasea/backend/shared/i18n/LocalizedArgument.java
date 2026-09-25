package com.danasea.backend.shared.i18n;

public interface LocalizedArgument {
    Object resolve(SupportedLanguage language);
}
