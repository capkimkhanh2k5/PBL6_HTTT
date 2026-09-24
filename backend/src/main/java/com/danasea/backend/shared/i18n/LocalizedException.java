package com.danasea.backend.shared.i18n;

public class LocalizedException extends RuntimeException {
    private final String errorCode;
    private final LocalizedMessageRef messageRef;

    public LocalizedException(String errorCode, String messageKey, Object... args) {
        super(errorCode);
        this.errorCode = errorCode;
        this.messageRef = LocalizedMessageRef.of(messageKey, args);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public LocalizedMessageRef getMessageRef() {
        return messageRef;
    }
}
