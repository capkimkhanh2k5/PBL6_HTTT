package com.danasea.backend.modules.weather.domain.models;

/**
 * Weather Alert Lifecycle Status Enum for DANASEA Marine Weather Monitoring System.
 */
public enum WeatherAlertStatus {
    MONITORING_YELLOW,
    AWAITING_ADMIN_RESOLUTION,
    AUTO_CANCELLED_FOR_SAFETY,
    RESOLVED_CANCEL_AND_REFUND,
    RESOLVED_DISMISSED,
    RESOLVED_SAFE
}
