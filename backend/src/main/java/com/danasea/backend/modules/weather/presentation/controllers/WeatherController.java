package com.danasea.backend.modules.weather.presentation.controllers;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.usecases.GetWeatherInfoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final GetWeatherInfoUseCase getWeatherInfoUseCase;

    @GetMapping("/current")
    @SecurityRequirements()
    public ResponseEntity<WeatherInfoDto> getCurrentWeatherAndMarine() {
        WeatherInfoDto weatherInfo = getWeatherInfoUseCase.execute();
        if (weatherInfo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(weatherInfo);
    }
}
