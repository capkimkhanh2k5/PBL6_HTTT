package com.danasea.backend.shared.i18n;

public record LocalizedMessageRef(String key, Object[] args) {
    public LocalizedMessageRef {
        args = args == null ? new Object[0] : args.clone();
    }

    public static LocalizedMessageRef of(String key, Object... args) {
        return new LocalizedMessageRef(key, args);
    }

    @Override
    public Object[] args() {
        return args.clone();
    }
}
