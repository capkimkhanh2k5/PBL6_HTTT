package com.danasea.backend.modules.weather.infrastructure.api;

import com.danasea.backend.modules.weather.infrastructure.api.dto.OpenMeteoMarineResponse;
import com.danasea.backend.modules.weather.infrastructure.api.dto.OpenMeteoWeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenMeteoApiClient {

        private final RestClient restClient;
        private final String weatherBaseUrl;
        private final String marineBaseUrl;

        public OpenMeteoApiClient(
                        RestClient.Builder restClientBuilder,
                        @Value("${open-meteo.base-url}") String weatherBaseUrl,
                        @Value("${open-meteo.marine-url}") String marineBaseUrl) {
                this.restClient = restClientBuilder.build();
                this.weatherBaseUrl = weatherBaseUrl;
                this.marineBaseUrl = marineBaseUrl;
        }

        public OpenMeteoWeatherResponse fetchWeather(double latitude, double longitude) {
                String url = String.format(
                                "%s/v1/forecast?latitude=%s&longitude=%s&current=temperature_2m,precipitation,wind_speed_10m,wind_gusts_10m,wind_direction_10m,visibility,cloud_cover,weather_code&hourly=precipitation_probability,uv_index&timezone=auto",
                                weatherBaseUrl, latitude, longitude);

                return restClient.get()
                                .uri(url)
                                .retrieve()
                                .body(OpenMeteoWeatherResponse.class);
        }

        public OpenMeteoMarineResponse fetchMarine(double latitude, double longitude) {
                String url = String.format(
                                "%s/v1/marine?latitude=%s&longitude=%s&hourly=wave_height,wave_direction,wave_period,swell_wave_height,swell_wave_direction,swell_wave_period,ocean_current_velocity,ocean_current_direction,sea_level_height_msl,sea_surface_temperature&models=best_match&timezone=auto",
                                marineBaseUrl, latitude, longitude);

                return restClient.get()
                                .uri(url)
                                .retrieve()
                                .body(OpenMeteoMarineResponse.class);
        }
}
