package com.danasea.backend.modules.weather.presentation.controllers;

import com.danasea.backend.modules.weather.application.dtos.AdvanceBookingSafetyRequest;
import com.danasea.backend.modules.weather.application.dtos.AdvanceBookingSafetyResponse;
import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.usecases.CheckAdvanceBookingSafetyUseCase;
import com.danasea.backend.modules.weather.application.usecases.GetWeatherInfoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping({"/api/weather", "/api/v1/weather"})
@RequiredArgsConstructor
public class WeatherController {

    private final GetWeatherInfoUseCase getWeatherInfoUseCase;
    private final CheckAdvanceBookingSafetyUseCase checkAdvanceBookingSafetyUseCase;

    @GetMapping("/current")
    @SecurityRequirements()
    public ResponseEntity<WeatherInfoDto> getCurrentWeatherAndMarine(
            @RequestParam(name = "lat", defaultValue = "16.089035780716284") double latitude,
            @RequestParam(name = "lng", defaultValue = "108.24959555394304") double longitude) {
        WeatherInfoDto weatherInfo = getWeatherInfoUseCase.execute(latitude, longitude);
        if (weatherInfo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(weatherInfo);
    }

    @GetMapping("/advance-safety-check")
    @SecurityRequirements()
    public ResponseEntity<AdvanceBookingSafetyResponse> checkAdvanceBookingSafety(
            @RequestParam(required = false) UUID slotId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false, defaultValue = "16.089035780716284") double latitude,
            @RequestParam(required = false, defaultValue = "108.24959555394304") double longitude,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime targetDateTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {

        if (slotId != null) {
            return ResponseEntity.ok(checkAdvanceBookingSafetyUseCase.checkBySlotId(slotId));
        }

        LocalDate resolvedDate = bookingDate != null
                ? bookingDate
                : targetDateTime != null ? targetDateTime.toLocalDate() : null;
        if (resolvedDate == null) {
            throw new IllegalArgumentException("Either bookingDate or targetDateTime is required");
        }

        LocalTime resolvedStartTime = startTime != null
                ? startTime
                : targetDateTime != null ? targetDateTime.toLocalTime() : LocalTime.of(8, 0);
        String resolvedCategorySlug = categoryId == null
                ? (categorySlug == null || categorySlug.isBlank() ? "cheo-sup-kayak" : categorySlug)
                : null;

        AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                .categoryId(categoryId)
                .categorySlug(resolvedCategorySlug)
                .latitude(latitude)
                .longitude(longitude)
                .bookingDate(resolvedDate)
                .startTime(resolvedStartTime)
                .endTime(endTime != null ? endTime : LocalTime.of(17, 0))
                .build();

        AdvanceBookingSafetyResponse response = checkAdvanceBookingSafetyUseCase.execute(request);
        return ResponseEntity.ok(response);
    }
}
