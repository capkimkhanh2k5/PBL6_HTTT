package com.danasea.backend.modules.weather.infrastructure.api;

import com.danasea.backend.modules.weather.infrastructure.api.dto.OpenMeteoMarineResponse;
import com.danasea.backend.modules.weather.infrastructure.api.dto.OpenMeteoWeatherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenMeteoApiClientTest {

    private OpenMeteoApiClient openMeteoApiClient;

    @BeforeEach
    void setUp() {
        // Init manually to avoid SpringBootContext failure without .env
        openMeteoApiClient = new OpenMeteoApiClient(
                RestClient.builder(),
                "https://api.open-meteo.com",
                "https://marine-api.open-meteo.com"
        );
    }

    @Test
    void shouldFetchWeatherSuccessfully() {
        // Da Nang coordinates
        double lat = 16.0544;
        double lon = 108.2022;

        OpenMeteoWeatherResponse response = openMeteoApiClient.fetchWeather(lat, lon);

        assertNotNull(response);
        assertNotNull(response.getCurrent());
        assertNotNull(response.getCurrent().getTemperature2m());
        System.out.println("Current Temp: " + response.getCurrent().getTemperature2m());
    }

    @Test
    void shouldFetchMarineSuccessfully() {
        // Da Nang coordinates
        double lat = 16.0544;
        double lon = 108.2022;

        OpenMeteoMarineResponse response = openMeteoApiClient.fetchMarine(lat, lon);

        assertNotNull(response);
        assertNotNull(response.getHourly());
        System.out.println("Hourly Marine Data count: " + response.getHourly().getTime().size());
        assertTrue(response.getHourly().getTime().size() > 0);
    }
}

