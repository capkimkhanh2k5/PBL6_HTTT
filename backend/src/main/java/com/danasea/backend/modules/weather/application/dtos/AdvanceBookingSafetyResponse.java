package com.danasea.backend.modules.weather.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvanceBookingSafetyResponse {

    // 1. Core Verdict
    private boolean isSafe;
    private String alertLevel; // GREEN, YELLOW, RED
    private String safetyStatus; // alias for alertLevel
    private boolean isProvisional;
    private boolean marineCutoffExceeded;

    // 2. Booking Context
    private UUID slotId;
    private UUID serviceId;
    private String categorySlug;
    private String categoryName;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private long daysInAdvance;
    private int daysAhead;

    // 3. Observed Peak Metrics in Window
    private Double peakWaveHeightM;
    private Double peakWindSpeedKmh;
    private Double peakWindGustKmh;
    private Double peakOceanCurrentMs;
    private Double minVisibilityM;
    private Integer severeWeatherCode;
    private Double totalPrecipitationMm;

    // 4. Thresholds from Category Safety Rule
    private Double cautionWaveHeightM;
    private Double maxWaveHeightM;
    private Double cautionWindSpeedKmh;
    private Double maxWindSpeedKmh;
    private Double maxWindGustKmh;
    private Double maxOceanCurrentMs;
    private Double ruleMinVisibilityM;

    // 5. Messages and Advisories
    private String warningMessage;
    private String summaryMessage;
    private List<String> advisoryDetails;
    private List<String> details;
    private List<String> advisoryNotes;

    // 6. Data Coverage & Confidence
    private String dataCoverage; // FULL_MARINE_AND_METEOROLOGY, METEOROLOGY_ESTIMATED_MARINE, OUT_OF_RANGE
    private String dataCoverageNote;
    private boolean estimatedMarine;
    private String confidenceLevel; // HIGH, MEDIUM, ADVISORY, LOW

    private WeatherInfoDto.TimeWindowForecast forecast;

    public String getSafetyStatus() {
        return safetyStatus != null ? safetyStatus : alertLevel;
    }

    public int getDaysAhead() {
        return daysAhead > 0 ? daysAhead : (int) daysInAdvance;
    }

    public String getSummaryMessage() {
        return summaryMessage != null ? summaryMessage : warningMessage;
    }

    public List<String> getDetails() {
        return details != null ? details : advisoryDetails;
    }

    public List<String> getAdvisoryNotes() {
        return advisoryNotes != null ? advisoryNotes : advisoryDetails;
    }
}
