package com.danasea.backend.modules.service.domain.exceptions;

public class WeatherRequirementsMissingException extends ServiceDomainException {

    public WeatherRequirementsMissingException() {
        super("Weather requirements (minWindKmh and maxWaveM) are required when weatherSensitive is true");
    }

    public WeatherRequirementsMissingException(String message) {
        super(message);
    }
}
