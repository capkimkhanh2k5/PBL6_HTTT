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
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final GetWeatherInfoUseCase getWeatherInfoUseCase;
    private final CheckAdvanceBookingSafetyUseCase checkAdvanceBookingSafetyUseCase;

    @GetMapping("/current")
    @SecurityRequirements()
    public ResponseEntity<WeatherInfoDto> getCurrentWeatherAndMarine() {
        WeatherInfoDto weatherInfo = getWeatherInfoUseCase.execute();
        if (weatherInfo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(weatherInfo);
    }

    @GetMapping("/advance-safety-check")
    @SecurityRequirements()
    public ResponseEntity<AdvanceBookingSafetyResponse> checkAdvanceBookingSafety(
            @RequestParam(required = false) UUID slotId,
            @RequestParam(required = false, defaultValue = "cheo-sup-kayak") String categorySlug,
            @RequestParam(required = false, defaultValue = "16.089035780716284") double latitude,
            @RequestParam(required = false, defaultValue = "108.24959555394304") double longitude,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {

        if (slotId != null) {
            return ResponseEntity.ok(checkAdvanceBookingSafetyUseCase.checkBySlotId(slotId));
        }

        if (bookingDate == null) {
            throw new IllegalArgumentException("Date is required");
        }

        AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                .categorySlug(categorySlug)
                .latitude(latitude)
                .longitude(longitude)
                .bookingDate(bookingDate)
                .startTime(startTime != null ? startTime : LocalTime.of(8, 0))
                .endTime(endTime != null ? endTime : LocalTime.of(17, 0))
                .build();

        AdvanceBookingSafetyResponse response = checkAdvanceBookingSafetyUseCase.execute(request);
        return ResponseEntity.ok(response);
    }
}
