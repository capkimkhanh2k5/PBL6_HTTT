package com.danasea.backend.modules.weather.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvanceBookingSafetyRequest {
    private String categorySlug;
    private UUID categoryId;
    private Double latitude;
    private Double longitude;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
