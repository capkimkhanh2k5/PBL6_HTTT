package com.danasea.backend.modules.ai;

import com.danasea.backend.modules.ai.application.tool.GetSafetyAlertTool;
import com.danasea.backend.modules.ai.application.tool.GetWeatherForecastTool;
import com.danasea.backend.modules.weather.application.usecases.GetWeatherInfoUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeatherAiToolsFailClosedTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void safetyToolDoesNotClaimSafeConditionsWhenProviderHasNoData() throws Exception {
        GetWeatherInfoUseCase weatherInfoUseCase = mock(GetWeatherInfoUseCase.class);
        when(weatherInfoUseCase.execute()).thenReturn(null);
        GetSafetyAlertTool tool = new GetSafetyAlertTool(weatherInfoUseCase, null, objectMapper);

        var response = objectMapper.readTree(tool.execute("{\"location\":\"Da Nang\"}"));

        assertThat(response.get("isSafe").asBoolean()).isFalse();
        assertThat(response.get("alertLevel").asText()).isEqualTo("YELLOW");
        assertThat(response.get("message").asText()).contains("unavailable");
    }

    @Test
    void forecastToolReturnsUnavailableInsteadOfFabricatedMeasurements() throws Exception {
        GetWeatherInfoUseCase weatherInfoUseCase = mock(GetWeatherInfoUseCase.class);
        when(weatherInfoUseCase.execute()).thenThrow(new IllegalStateException("provider offline"));
        GetWeatherForecastTool tool = new GetWeatherForecastTool(weatherInfoUseCase, null, objectMapper);

        var response = objectMapper.readTree(tool.execute("{\"location\":\"Da Nang\",\"date\":\"today\"}"));

        assertThat(response.get("available").asBoolean()).isFalse();
        assertThat(response.get("message").asText()).contains("temporarily unavailable");
        assertThat(response.has("temperature")).isFalse();
        assertThat(response.has("waveHeight")).isFalse();
    }
}
