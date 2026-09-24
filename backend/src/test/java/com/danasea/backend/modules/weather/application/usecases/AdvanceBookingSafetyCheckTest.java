package com.danasea.backend.modules.weather.application.usecases;

import com.danasea.backend.modules.service.infrastructure.persistence.entities.CategoryJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaCategoryRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import com.danasea.backend.modules.weather.application.dtos.AdvanceBookingSafetyRequest;
import com.danasea.backend.modules.weather.application.dtos.AdvanceBookingSafetyResponse;
import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.ports.output.WeatherProviderPort;
import com.danasea.backend.modules.weather.domain.models.CategorySafetyRule;
import com.danasea.backend.modules.weather.domain.services.CategorySafetyRuleService;
import com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine;
import com.danasea.backend.modules.weather.infrastructure.adapters.WeatherForecastCacheService;
import com.danasea.backend.modules.weather.infrastructure.adapters.WeatherProviderAdapter;
import com.danasea.backend.modules.weather.infrastructure.api.OpenMeteoApiClient;
import com.danasea.backend.modules.weather.infrastructure.api.dto.OpenMeteoMarineResponse;
import com.danasea.backend.modules.weather.infrastructure.api.dto.OpenMeteoWeatherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdvanceBookingSafetyCheckTest - Comprehensive 7-14 Day Advance Reservation Suite")
public class AdvanceBookingSafetyCheckTest {

    @Mock
    private CategorySafetyRuleService categorySafetyRuleService;

    @Mock
    private WeatherProviderPort weatherProviderPort;

    @Mock
    private JpaServiceSlotRepository slotRepository;

    @Mock
    private JpaServiceRepository serviceRepository;

    @Mock
    private JpaCategoryRepository categoryRepository;

    private WeatherRuleEngine weatherRuleEngine;
    private CheckAdvanceBookingSafetyUseCase useCase;

    private final LocalDate today = LocalDate.of(2026, 9, 20);
    private CategorySafetyRule supRule;
    private CategorySafetyRule divingRule;

    @BeforeEach
    void setUp() {
        weatherRuleEngine = new WeatherRuleEngine();
        useCase = new CheckAdvanceBookingSafetyUseCase(
                categorySafetyRuleService,
                weatherProviderPort,
                weatherRuleEngine,
                slotRepository,
                serviceRepository,
                categoryRepository,
                Clock.fixed(today.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
                        ZoneId.of("Asia/Ho_Chi_Minh"))
        );

        supRule = CategorySafetyRule.getBySlug("cheo-sup-kayak");
        divingRule = CategorySafetyRule.getBySlug("lan-ngam-san-ho");
    }

    @Nested
    @DisplayName("Explorer 3 Proposed Test Matrix (TC1 - TC13)")
    class Explorer3TestMatrix {

        @Test
        @DisplayName("TC1: Day 7 - Ideal weather & marine -> Returns GREEN, isProvisional=false, marineCutoffExceeded=false")
        void testAdvanceCheck_Day7_IdealWeatherAndMarine_ReturnsGreen() {
            LocalDate bookingDate = today.plusDays(7);
            when(categorySafetyRuleService.getRuleByCategorySlug("cheo-sup-kayak")).thenReturn(supRule);

            WeatherInfoDto.TimeWindowForecast forecast = WeatherInfoDto.TimeWindowForecast.builder()
                    .peakWaveHeight(0.40)
                    .peakWindSpeed(10.0)
                    .peakWindGust(15.0)
                    .peakOceanCurrent(0.15)
                    .minVisibility(8000.0)
                    .severeWeatherCode(1)
                    .totalPrecipitation(0.0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), eq(bookingDate), any(), any()))
                    .thenReturn(forecast);

            AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                    .categorySlug("cheo-sup-kayak")
                    .bookingDate(bookingDate)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(10, 0))
                    .build();

            AdvanceBookingSafetyResponse response = useCase.execute(request, today);

            assertNotNull(response);
            assertTrue(response.isSafe());
            assertEquals("GREEN", response.getSafetyStatus());
            assertEquals(WeatherRuleEngine.ALERT_GREEN, response.getAlertLevel());
            assertFalse(response.isProvisional(), "Day 7 has full marine data, isProvisional must be false");
            assertFalse(response.isMarineCutoffExceeded(), "Day 7 is within 8-day marine cutoff");
            assertNotNull(response.getPeakWaveHeightM());
            assertEquals(0.40, response.getPeakWaveHeightM());
            assertEquals(7, response.getDaysAhead());
        }

        @Test
        @DisplayName("TC2: Day 8 - High wave (1.3m > 0.8m max) -> Returns RED, isProvisional=false, details contains wave violation")
        void testAdvanceCheck_Day8_HighWave_ReturnsRed() {
            LocalDate bookingDate = today.plusDays(8);
            when(categorySafetyRuleService.getRuleByCategorySlug("cheo-sup-kayak")).thenReturn(supRule);

            WeatherInfoDto.TimeWindowForecast forecast = WeatherInfoDto.TimeWindowForecast.builder()
                    .peakWaveHeight(1.30)
                    .peakWindSpeed(15.0)
                    .peakWindGust(20.0)
                    .peakOceanCurrent(0.20)
                    .minVisibility(6000.0)
                    .severeWeatherCode(2)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), eq(bookingDate), any(), any()))
                    .thenReturn(forecast);

            AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                    .categorySlug("cheo-sup-kayak")
                    .bookingDate(bookingDate)
                    .build();

            AdvanceBookingSafetyResponse response = useCase.execute(request, today);

            assertNotNull(response);
            assertFalse(response.isSafe());
            assertEquals("RED", response.getSafetyStatus());
            assertEquals(WeatherRuleEngine.ALERT_RED, response.getAlertLevel());
            assertFalse(response.isProvisional());
            assertFalse(response.isMarineCutoffExceeded());
            assertEquals(1.30, response.getPeakWaveHeightM());
            assertTrue(response.getWarningMessage().contains("Wave height 1.3m exceeds the maximum safe threshold"));
        }

        @Test
        @DisplayName("TC3: Day 7 - Caution wind (15 km/h > 12 km/h caution, <= 20 km/h max) -> Returns YELLOW")
        void testAdvanceCheck_Day7_CautionWind_ReturnsYellow() {
            LocalDate bookingDate = today.plusDays(7);
            when(categorySafetyRuleService.getRuleByCategorySlug("cheo-sup-kayak")).thenReturn(supRule);

            WeatherInfoDto.TimeWindowForecast forecast = WeatherInfoDto.TimeWindowForecast.builder()
                    .peakWaveHeight(0.40)
                    .peakWindSpeed(15.0)
                    .peakWindGust(18.0)
                    .peakOceanCurrent(0.15)
                    .minVisibility(7000.0)
                    .severeWeatherCode(1)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), eq(bookingDate), any(), any()))
                    .thenReturn(forecast);

            AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                    .categorySlug("cheo-sup-kayak")
                    .bookingDate(bookingDate)
                    .build();

            AdvanceBookingSafetyResponse response = useCase.execute(request, today);

            assertNotNull(response);
            assertTrue(response.isSafe());
            assertEquals("YELLOW", response.getSafetyStatus());
            assertEquals(WeatherRuleEngine.ALERT_YELLOW, response.getAlertLevel());
            assertFalse(response.isProvisional());
            assertFalse(response.isMarineCutoffExceeded());
            assertTrue(response.getWarningMessage().contains("CAUTION ALERT"));
        }

        @Test
        @DisplayName("TC4: Day 10 - Gentle atmospheric weather, marine wave is null -> Returns GREEN, isProvisional=true, marineCutoffExceeded=true")
        void testAdvanceCheck_Day10_GentleWeather_ReturnsGreenProvisional() {
            LocalDate bookingDate = today.plusDays(10);
            when(categorySafetyRuleService.getRuleByCategorySlug("cheo-sup-kayak")).thenReturn(supRule);

            // Day 10: beyond day 8, Marine API wave & current are null
            WeatherInfoDto.TimeWindowForecast forecast = WeatherInfoDto.TimeWindowForecast.builder()
                    .peakWaveHeight(null)
                    .peakWindSpeed(10.0)
                    .peakWindGust(15.0)
                    .peakOceanCurrent(null)
                    .minVisibility(5000.0)
                    .severeWeatherCode(0)
                    .totalPrecipitation(0.0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), eq(bookingDate), any(), any()))
                    .thenReturn(forecast);

            AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                    .categorySlug("cheo-sup-kayak")
                    .bookingDate(bookingDate)
                    .build();

            AdvanceBookingSafetyResponse response = useCase.execute(request, today);

            assertNotNull(response);
            assertTrue(response.isSafe());
            assertEquals("GREEN", response.getSafetyStatus());
            assertTrue(response.isProvisional(), "Beyond day 8, isProvisional must be true");
            assertTrue(response.isMarineCutoffExceeded(), "Beyond day 8, marineCutoffExceeded must be true");
            assertNull(response.getPeakWaveHeightM(), "Marine cutoff exceeded, peakWaveHeightM must be null");
            assertTrue(response.getAdvisoryNotes().stream().anyMatch(n -> n.contains("8 days before departure")));
            assertTrue(response.getSummaryMessage().contains("PROVISIONALLY SAFE FORECAST"));
        }

        @Test
        @DisplayName("TC5: Day 12 - Severe wind gust (45 km/h > 28 km/h max) with null wave -> Returns RED")
        void testAdvanceCheck_Day12_SevereWindGust_ReturnsRed() {
            LocalDate bookingDate = today.plusDays(12);
            when(categorySafetyRuleService.getRuleByCategorySlug("cheo-sup-kayak")).thenReturn(supRule);

            WeatherInfoDto.TimeWindowForecast forecast = WeatherInfoDto.TimeWindowForecast.builder()
                    .peakWaveHeight(null)
                    .peakWindSpeed(18.0)
                    .peakWindGust(45.0)
                    .minVisibility(4000.0)
                    .severeWeatherCode(61)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), eq(bookingDate), any(), any()))
                    .thenReturn(forecast);

            AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                    .categorySlug("cheo-sup-kayak")
                    .bookingDate(bookingDate)
                    .build();

            AdvanceBookingSafetyResponse response = useCase.execute(request, today);

            assertNotNull(response);
            assertFalse(response.isSafe());
            assertEquals("RED", response.getSafetyStatus());
            assertTrue(response.isProvisional());
            assertTrue(response.isMarineCutoffExceeded());
            assertTrue(response.getDetails().stream().anyMatch(d -> d.contains("Peak wind gust 45.0 km/h exceeds the aerodynamic safety limit")));
        }

        @Test
        @DisplayName("TC6: Day 14 - Fatal thunderstorm WMO 95 code with null wave -> Returns RED")
        void testAdvanceCheck_Day14_FatalThunderstormWmo95_ReturnsRed() {
            LocalDate bookingDate = today.plusDays(14);
            when(categorySafetyRuleService.getRuleByCategorySlug("lan-ngam-san-ho")).thenReturn(divingRule);

            WeatherInfoDto.TimeWindowForecast forecast = WeatherInfoDto.TimeWindowForecast.builder()
                    .peakWaveHeight(null)
                    .peakWindSpeed(12.0)
                    .peakWindGust(18.0)
                    .minVisibility(3000.0)
                    .severeWeatherCode(95) // Thunderstorm
                    .totalPrecipitation(25.0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), eq(bookingDate), any(), any()))
                    .thenReturn(forecast);

            AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                    .categorySlug("lan-ngam-san-ho")
                    .bookingDate(bookingDate)
                    .build();

            AdvanceBookingSafetyResponse response = useCase.execute(request, today);

            assertNotNull(response);
            assertFalse(response.isSafe());
            assertEquals("RED", response.getSafetyStatus());
            assertTrue(response.isProvisional());
            assertTrue(response.isMarineCutoffExceeded());
            assertTrue(response.getWarningMessage().contains("thunderstorm"));
        }

        @Test
        @DisplayName("TC7: Day 11 - Caution wind (16 km/h > 12 km/h caution) -> Returns YELLOW provisional")
        void testAdvanceCheck_Day11_CautionWind_ReturnsYellowProvisional() {
            LocalDate bookingDate = today.plusDays(11);
            when(categorySafetyRuleService.getRuleByCategorySlug("cheo-sup-kayak")).thenReturn(supRule);

            WeatherInfoDto.TimeWindowForecast forecast = WeatherInfoDto.TimeWindowForecast.builder()
                    .peakWaveHeight(null)
                    .peakWindSpeed(16.0)
                    .peakWindGust(20.0)
                    .minVisibility(6000.0)
                    .severeWeatherCode(2)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), eq(bookingDate), any(), any()))
                    .thenReturn(forecast);

            AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                    .categorySlug("cheo-sup-kayak")
                    .bookingDate(bookingDate)
                    .build();

            AdvanceBookingSafetyResponse response = useCase.execute(request, today);

            assertNotNull(response);
            assertTrue(response.isSafe());
            assertEquals("YELLOW", response.getSafetyStatus());
            assertTrue(response.isProvisional());
            assertTrue(response.isMarineCutoffExceeded());
            assertTrue(response.getWarningMessage().contains("CAUTION ALERT"));
        }

        @Test
        @DisplayName("TC8: Day 8 Boundary Day - Marine available -> marineCutoffExceeded=false, peakWaveHeightM != null")
        void testAdvanceCheck_Day8_BoundaryDay_MarineAvailable() {
            LocalDate bookingDate = today.plusDays(8);
            when(categorySafetyRuleService.getRuleByCategorySlug("cheo-sup-kayak")).thenReturn(supRule);

            WeatherInfoDto.TimeWindowForecast forecast = WeatherInfoDto.TimeWindowForecast.builder()
                    .peakWaveHeight(0.50)
                    .peakWindSpeed(10.0)
                    .peakWindGust(15.0)
                    .peakOceanCurrent(0.12)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), eq(bookingDate), any(), any()))
                    .thenReturn(forecast);

            AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                    .categorySlug("cheo-sup-kayak")
                    .bookingDate(bookingDate)
                    .build();

            AdvanceBookingSafetyResponse response = useCase.execute(request, today);

            assertNotNull(response);
            assertFalse(response.isMarineCutoffExceeded(), "Day 8 must not exceed marine cutoff");
            assertFalse(response.isProvisional(), "Day 8 must not be provisional");
            assertNotNull(response.getPeakWaveHeightM());
            assertEquals(0.50, response.getPeakWaveHeightM());
        }

        @Test
        @DisplayName("TC9: Day 9 Boundary Day - Marine exceeded -> marineCutoffExceeded=true, isProvisional=true, peakWaveHeightM == null")
        void testAdvanceCheck_Day9_BoundaryDay_MarineExceeded() {
            LocalDate bookingDate = today.plusDays(9);
            when(categorySafetyRuleService.getRuleByCategorySlug("cheo-sup-kayak")).thenReturn(supRule);

            WeatherInfoDto.TimeWindowForecast forecast = WeatherInfoDto.TimeWindowForecast.builder()
                    .peakWaveHeight(null)
                    .peakWindSpeed(10.0)
                    .peakWindGust(15.0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), eq(bookingDate), any(), any()))
                    .thenReturn(forecast);

            AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                    .categorySlug("cheo-sup-kayak")
                    .bookingDate(bookingDate)
                    .build();

            AdvanceBookingSafetyResponse response = useCase.execute(request, today);

            assertNotNull(response);
            assertTrue(response.isMarineCutoffExceeded(), "Day 9 must exceed marine cutoff");
            assertTrue(response.isProvisional(), "Day 9 must be provisional");
            assertNull(response.getPeakWaveHeightM(), "Day 9 marine data is not directly available");
        }

        @Test
        @DisplayName("TC10: SlotId Resolution - Loads Service, Slot, and Category correctly")
        void testAdvanceCheck_SlotIdResolution_LoadsServiceAndCategory() {
            UUID slotId = UUID.randomUUID();
            UUID serviceId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            LocalDate bookingDate = today.plusDays(7);

            ServiceSlotJpaEntity mockSlot = new ServiceSlotJpaEntity();
            mockSlot.setId(slotId);
            mockSlot.setServiceId(serviceId);
            mockSlot.setDate(bookingDate);
            mockSlot.setStartTime(LocalTime.of(8, 30));
            mockSlot.setEndTime(LocalTime.of(10, 30));

            ServiceJpaEntity mockService = new ServiceJpaEntity();
            mockService.setId(serviceId);
            mockService.setCategoryId(categoryId);
            mockService.setLatitude(BigDecimal.valueOf(16.0890));
            mockService.setLongitude(BigDecimal.valueOf(108.2495));

            CategoryJpaEntity mockCategory = new CategoryJpaEntity();
            mockCategory.setId(categoryId);
            mockCategory.setSlug("cheo-sup-kayak");
            mockCategory.setName("Chèo SUP & Kayak");

            when(slotRepository.findById(slotId)).thenReturn(Optional.of(mockSlot));
            when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(mockService));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(mockCategory));
            when(categorySafetyRuleService.getRuleByCategorySlug("cheo-sup-kayak")).thenReturn(supRule);

            WeatherInfoDto.TimeWindowForecast forecast = WeatherInfoDto.TimeWindowForecast.builder()
                    .peakWaveHeight(0.45)
                    .peakWindSpeed(11.0)
                    .peakWindGust(16.0)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(eq(16.0890), eq(108.2495), eq(bookingDate), any(), any()))
                    .thenReturn(forecast);

            AdvanceBookingSafetyResponse response = useCase.checkBySlotId(slotId);

            assertNotNull(response);
            assertEquals(slotId, response.getSlotId());
            assertEquals(serviceId, response.getServiceId());
            assertEquals("cheo-sup-kayak", response.getCategorySlug());
            assertTrue(response.isSafe());
        }

        @Test
        @DisplayName("TC11: Past booking date -> Throws IllegalArgumentException")
        void testAdvanceCheck_PastDate_ThrowsIllegalArgumentException() {
            LocalDate pastDate = today.minusDays(1);
            AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                    .categorySlug("cheo-sup-kayak")
                    .bookingDate(pastDate)
                    .build();

            assertThrows(IllegalArgumentException.class, () -> useCase.execute(request, today));
        }

        @Test
        @DisplayName("TC12: checkSafety with Beyond 16 Days -> Throws IllegalArgumentException")
        void testAdvanceCheck_Beyond16Days_ThrowsIllegalArgumentException() {
            LocalDate distantDate = today.plusDays(18);

            assertThrows(IllegalArgumentException.class, () -> useCase.checkSafety(
                    UUID.randomUUID(), UUID.randomUUID(), "cheo-sup-kayak",
                    distantDate, LocalTime.of(8, 0), LocalTime.of(10, 0),
                    16.089, 108.249
            ));
        }

        @Test
        @DisplayName("TC13: Unknown category slug -> Falls back to DEFAULT_RULE without throwing 500")
        void testAdvanceCheck_UnknownCategory_FallsBackToDefaultRule() {
            LocalDate bookingDate = today.plusDays(5);
            when(categorySafetyRuleService.getRuleByCategorySlug("unknown-sport"))
                    .thenReturn(CategorySafetyRule.DEFAULT_RULE);

            WeatherInfoDto.TimeWindowForecast forecast = WeatherInfoDto.TimeWindowForecast.builder()
                    .peakWaveHeight(0.50)
                    .peakWindSpeed(12.0)
                    .peakWindGust(16.0)
                    .minVisibility(5000.0)
                    .severeWeatherCode(1)
                    .build();

            when(weatherProviderPort.getTimeWindowForecast(anyDouble(), anyDouble(), eq(bookingDate), any(), any()))
                    .thenReturn(forecast);

            AdvanceBookingSafetyRequest request = AdvanceBookingSafetyRequest.builder()
                    .categorySlug("unknown-sport")
                    .bookingDate(bookingDate)
                    .build();

            AdvanceBookingSafetyResponse response = useCase.execute(request, today);

            assertNotNull(response);
            assertTrue(response.isSafe());
            assertEquals("default", response.getCategorySlug());
            assertEquals(CategorySafetyRule.DEFAULT_RULE.getCategoryName(), response.getCategoryName());
        }
    }

    @Nested
    @DisplayName("Multi-Day Forecast Caching & Rate-Limit Prevention Integration")
    class MultiDayForecastCachingTests {

        @Mock
        private OpenMeteoApiClient mockApiClient;

        @Test
        @DisplayName("Calling getTimeWindowForecast for multiple advance booking queries hits Open-Meteo once and uses cache")
        void getTimeWindowForecast_usesCacheForMultipleQueries() {
            WeatherForecastCacheService cacheService = new WeatherForecastCacheService();
            WeatherProviderAdapter adapter = new WeatherProviderAdapter(mockApiClient, cacheService);

            OpenMeteoWeatherResponse weatherResp = new OpenMeteoWeatherResponse();
            OpenMeteoWeatherResponse.HourlyData hourlyWeather = new OpenMeteoWeatherResponse.HourlyData();
            hourlyWeather.setTime(List.of("2026-09-25T08:00", "2026-09-25T09:00"));
            hourlyWeather.setWindSpeed10m(List.of(12.0, 14.0));
            hourlyWeather.setWindGusts10m(List.of(18.0, 20.0));
            hourlyWeather.setVisibility(List.of(8000.0, 8000.0));
            hourlyWeather.setPrecipitation(List.of(0.0, 0.0));
            weatherResp.setHourly(hourlyWeather);

            OpenMeteoMarineResponse marineResp = new OpenMeteoMarineResponse();
            OpenMeteoMarineResponse.HourlyData hourlyMarine = new OpenMeteoMarineResponse.HourlyData();
            hourlyMarine.setTime(List.of("2026-09-25T08:00", "2026-09-25T09:00"));
            hourlyMarine.setWaveHeight(List.of(0.5, 0.6));
            marineResp.setHourly(hourlyMarine);

            when(mockApiClient.fetchWeather(16.0890, 108.2495)).thenReturn(weatherResp);
            when(mockApiClient.fetchMarine(16.0890, 108.2495)).thenReturn(marineResp);

            // Call 1: Morning slot on Sept 25
            WeatherInfoDto.TimeWindowForecast slot1 = adapter.getTimeWindowForecast(
                    16.0890, 108.2495, LocalDate.of(2026, 9, 25), LocalTime.of(8, 0), LocalTime.of(9, 0)
            );
            assertNotNull(slot1);
            assertEquals(14.0, slot1.getPeakWindSpeed());
            assertEquals(0.6, slot1.getPeakWaveHeight());

            // Call 2: Afternoon slot on Sept 25 (same location)
            WeatherInfoDto.TimeWindowForecast slot2 = adapter.getTimeWindowForecast(
                    16.0890, 108.2495, LocalDate.of(2026, 9, 25), LocalTime.of(8, 0), LocalTime.of(9, 0)
            );
            assertNotNull(slot2);

            // Verify API was called EXACTLY ONCE!
            verify(mockApiClient, times(1)).fetchWeather(16.0890, 108.2495);
            verify(mockApiClient, times(1)).fetchMarine(16.0890, 108.2495);
            assertTrue(adapter.isWeatherCached(16.0890, 108.2495));
            assertTrue(adapter.isMarineCached(16.0890, 108.2495));

            // Test Invalidation
            adapter.invalidateCache(16.0890, 108.2495);
            assertFalse(adapter.isWeatherCached(16.0890, 108.2495));
            assertFalse(adapter.isMarineCached(16.0890, 108.2495));
        }
    }
}
