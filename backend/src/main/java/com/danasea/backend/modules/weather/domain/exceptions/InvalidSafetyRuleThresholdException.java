package com.danasea.backend.modules.weather.domain.exceptions;

public class InvalidSafetyRuleThresholdException extends RuntimeException {
    public InvalidSafetyRuleThresholdException(String message) {
        super(message);
    }
}
