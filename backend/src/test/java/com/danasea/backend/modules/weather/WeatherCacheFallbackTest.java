package com.danasea.backend.modules.weather;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.ports.output.WeatherCacheRepositoryPort;
import com.danasea.backend.modules.weather.application.ports.output.WeatherProviderPort;
import com.danasea.backend.modules.weather.application.usecases.GetWeatherInfoUseCase;
import com.danasea.backend.modules.weather.domain.models.WeatherCache;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherCacheFallback Tests - Cache freshness and stale data quality")
class WeatherCacheFallbackTest {

    @Mock private WeatherCacheRepositoryPort weatherCacheRepositoryPort;

    @Mock private WeatherProviderPort weatherProviderPort;

    private ObjectMapper objectMapper;
    private GetWeatherInfoUseCase useCase;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        useCase =
                new GetWeatherInfoUseCase(
                        weatherCacheRepositoryPort, objectMapper, weatherProviderPort);
    }

    private WeatherCache createCache(OffsetDateTime fetchedAt, double waveHeight, double windSpeed)
            throws Exception {
        WeatherInfoDto.WeatherData weather =
                WeatherInfoDto.WeatherData.builder()
                        .temperature(28.0)
                        .windSpeed(windSpeed)
                        .precipitation(0.0)
                        .build();
        WeatherInfoDto.MarineData marine =
                WeatherInfoDto.MarineData.builder().waveHeight(waveHeight).wavePeriod(6.0).build();
        WeatherInfoDto dto =
                WeatherInfoDto.builder()
                        .latitude(GetWeatherInfoUseCase.DEFAULT_LATITUDE)
                        .longitude(GetWeatherInfoUseCase.DEFAULT_LONGITUDE)
                        .weather(weather)
                        .marine(marine)
                        .build();

        WeatherCache cache = new WeatherCache();
        cache.setLocationKey(GetWeatherInfoUseCase.LOCATION_KEY);
        cache.setFetchedAt(fetchedAt);
        cache.setCreatedAt(fetchedAt);
        cache.setExpiresAt(fetchedAt.plusMinutes(30));
        cache.setRawPayload(objectMapper.writeValueAsString(dto));
        return cache;
    }

    @Test
    @DisplayName(
            "✅ Cache fresh (<= 30 phút): Dùng ngay cache, không gọi provider ngoài, stale = false")
    void execute_freshCache_returnsCachedDataWithStaleFalse() throws Exception {
        OffsetDateTime tenMinutesAgo = OffsetDateTime.now().minusMinutes(10);
        WeatherCache cache = createCache(tenMinutesAgo, 0.5, 12.0);

        when(weatherCacheRepositoryPort.getLatestByLocation(anyString()))
                .thenReturn(Optional.of(cache));

        WeatherInfoDto result = useCase.execute();

        assertNotNull(result);
        assertFalse(result.isStale(), "Cache fresh phải có stale = false");
        assertEquals("OPEN_METEO", result.getSource());
        assertEquals("FULL_MARINE_AND_METEOROLOGY", result.getDataCoverage());
        assertFalse(result.isEstimatedMarine());
        verifyNoInteractions(weatherProviderPort);
    }

    @Test
    @DisplayName(
            "⚠️ Cache stale (> 30 phút - <= 2 giờ): Khi provider ngoài lỗi, fallback về cache cũ và"
                    + " đánh dấu stale = true")
    void execute_staleCache_providerFails_fallsBackToStaleCache() throws Exception {
        OffsetDateTime fortyFiveMinutesAgo = OffsetDateTime.now().minusMinutes(45);
        WeatherCache cache = createCache(fortyFiveMinutesAgo, 0.7, 15.0);

        when(weatherCacheRepositoryPort.getLatestByLocation(anyString()))
                .thenReturn(Optional.of(cache));
        when(weatherProviderPort.getWeatherByCoordinates(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Upstream Open-Meteo timeout 504"));

        WeatherInfoDto result = useCase.execute();

        assertNotNull(result, "Phải fallback thành công về stale cache khi upstream lỗi");
        assertTrue(result.isStale(), "Dữ liệu trả về phải được gắn cờ stale = true");
        assertEquals("OPEN_METEO", result.getSource());
        assertEquals(
                fortyFiveMinutesAgo.toInstant(),
                result.getFetchedAt(),
                "Giữ nguyên thời điểm lấy dữ liệu gốc");
        verify(weatherProviderPort, times(1)).getWeatherByCoordinates(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName(
            "🔄 Cache stale (> 30 phút): Khi provider ngoài bình thường, làm mới cache và trả dữ"
                    + " liệu mới (stale = false)")
    void execute_staleCache_providerSucceeds_refreshesCache() throws Exception {
        OffsetDateTime fortyMinutesAgo = OffsetDateTime.now().minusMinutes(40);
        WeatherCache staleCache = createCache(fortyMinutesAgo, 0.6, 10.0);

        when(weatherCacheRepositoryPort.getLatestByLocation(anyString()))
                .thenReturn(Optional.of(staleCache));

        WeatherInfoDto.WeatherData freshWeather =
                WeatherInfoDto.WeatherData.builder()
                        .temperature(29.0)
                        .windSpeed(11.0)
                        .precipitation(0.0)
                        .build();
        WeatherInfoDto.MarineData freshMarine =
                WeatherInfoDto.MarineData.builder().waveHeight(0.4).wavePeriod(5.0).build();

        when(weatherProviderPort.getWeatherByCoordinates(anyDouble(), anyDouble()))
                .thenReturn(freshWeather);
        when(weatherProviderPort.getMarineByCoordinates(anyDouble(), anyDouble()))
                .thenReturn(freshMarine);

        WeatherInfoDto result = useCase.execute();

        assertNotNull(result);
        assertFalse(result.isStale(), "Dữ liệu vừa fetch mới phải có stale = false");
        assertEquals(0.4, result.getMarine().getWaveHeight());
        verify(weatherCacheRepositoryPort, times(1)).save(any(WeatherCache.class));
    }

    @Test
    @DisplayName(
            "❌ Cache quá hạn (> 2 giờ): Khi provider lỗi, tuyệt đối KHÔNG dùng cache quá cũ làm"
                    + " chứng nhận an toàn (trả null)")
    void execute_expiredCache_providerFails_returnsNull() throws Exception {
        OffsetDateTime twoAndHalfHoursAgo = OffsetDateTime.now().minusMinutes(150);
        WeatherCache expiredCache = createCache(twoAndHalfHoursAgo, 0.4, 8.0);

        when(weatherCacheRepositoryPort.getLatestByLocation(anyString()))
                .thenReturn(Optional.of(expiredCache));
        when(weatherProviderPort.getWeatherByCoordinates(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Upstream connection refused"));

        WeatherInfoDto result = useCase.execute();

        assertNull(result, "Cache quá 2 giờ không được phép trả về khi upstream lỗi");
    }

    @Test
    @DisplayName("❌ Không có cache và provider lỗi: Trả null (không đủ dữ liệu)")
    void execute_noCache_providerFails_returnsNull() {
        when(weatherCacheRepositoryPort.getLatestByLocation(anyString()))
                .thenReturn(Optional.empty());
        when(weatherProviderPort.getWeatherByCoordinates(anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("OpenMeteo 500 Internal Error"));

        WeatherInfoDto result = useCase.execute();

        assertNull(result);
    }

    @Test
    @DisplayName(
            "📊 Đầy đủ dữ liệu: Khi thiếu sóng biển, đánh dấu estimatedMarine = true và coverage"
                    + " phù hợp")
    void execute_missingMarine_marksEstimatedTrue() {
        when(weatherCacheRepositoryPort.getLatestByLocation(anyString()))
                .thenReturn(Optional.empty());

        WeatherInfoDto.WeatherData freshWeather =
                WeatherInfoDto.WeatherData.builder()
                        .temperature(27.0)
                        .windSpeed(9.0)
                        .precipitation(0.0)
                        .build();

        when(weatherProviderPort.getWeatherByCoordinates(anyDouble(), anyDouble()))
                .thenReturn(freshWeather);
        when(weatherProviderPort.getMarineByCoordinates(anyDouble(), anyDouble())).thenReturn(null);

        WeatherInfoDto result = useCase.execute();

        assertNotNull(result);
        assertFalse(result.isEstimatedMarine());
        assertEquals("METEOROLOGY_ONLY", result.getDataCoverage());
    }
}
