package com.danasea.backend.modules.weather.application.ports.output;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;

import java.time.LocalDate;
import java.time.LocalTime;

public interface WeatherProviderPort {
    WeatherInfoDto.WeatherData getWeatherByCoordinates(double latitude, double longitude);
    WeatherInfoDto.MarineData getMarineByCoordinates(double latitude, double longitude);
    WeatherInfoDto.TimeWindowForecast getTimeWindowForecast(double latitude, double longitude, LocalDate date, LocalTime startTime, LocalTime endTime);
}
