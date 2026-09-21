package com.danasea.backend.modules.weather.domain.exceptions;

public class CategorySafetyRuleNotFoundException extends RuntimeException {
    public CategorySafetyRuleNotFoundException(String message) {
        super(message);
    }
}
