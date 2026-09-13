package com.danasea.backend.modules.weather.application.ports.output;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;

public interface WeatherProviderPort {
    WeatherInfoDto.WeatherData getWeatherByCoordinates(double latitude, double longitude);
    WeatherInfoDto.MarineData getMarineByCoordinates(double latitude, double longitude);
}
