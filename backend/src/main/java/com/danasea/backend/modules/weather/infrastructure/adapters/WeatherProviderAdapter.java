package com.danasea.backend.modules.weather.infrastructure.adapters;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.ports.output.WeatherProviderPort;
import com.danasea.backend.modules.weather.infrastructure.api.OpenMeteoApiClient;
import com.danasea.backend.modules.weather.infrastructure.api.dto.OpenMeteoMarineResponse;
import com.danasea.backend.modules.weather.infrastructure.api.dto.OpenMeteoWeatherResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class WeatherProviderAdapter implements WeatherProviderPort {

    private final OpenMeteoApiClient apiClient;
    private final WeatherForecastCacheService cacheService;

    @Autowired
    public WeatherProviderAdapter(OpenMeteoApiClient apiClient, WeatherForecastCacheService cacheService) {
        this.apiClient = apiClient;
        this.cacheService = cacheService != null ? cacheService : new WeatherForecastCacheService();
    }

    public WeatherProviderAdapter(OpenMeteoApiClient apiClient) {
        this(apiClient, new WeatherForecastCacheService());
    }

    @Override
    public WeatherInfoDto.WeatherData getWeatherByCoordinates(double latitude, double longitude) {
        OpenMeteoWeatherResponse response = cacheService.getOrFetchWeather(
                latitude, longitude, () -> apiClient.fetchWeather(latitude, longitude));

        if (response == null || response.getCurrent() == null) {
            return null;
        }

        OpenMeteoWeatherResponse.CurrentData current = response.getCurrent();
        OpenMeteoWeatherResponse.HourlyData hourly = response.getHourly();

        Integer precipProb = null;
        Double uvIndex = current.getUvIndex();
        Double relHumidity = current.getRelativeHumidity2m();
        Double dewPoint = current.getDewPoint2m();
        Double surfacePressure = current.getSurfacePressure();

        if (hourly != null) {
            if (hourly.getPrecipitationProbability() != null && !hourly.getPrecipitationProbability().isEmpty()) {
                precipProb = hourly.getPrecipitationProbability().get(0);
            }
            if (uvIndex == null && hourly.getUvIndex() != null && !hourly.getUvIndex().isEmpty()) {
                uvIndex = hourly.getUvIndex().get(0);
            }
            if (relHumidity == null && hourly.getRelativeHumidity2m() != null && !hourly.getRelativeHumidity2m().isEmpty()) {
                Integer h = hourly.getRelativeHumidity2m().get(0);
                relHumidity = h != null ? h.doubleValue() : null;
            }
            if (dewPoint == null && hourly.getDewPoint2m() != null && !hourly.getDewPoint2m().isEmpty()) {
                dewPoint = hourly.getDewPoint2m().get(0);
            }
            if (surfacePressure == null && hourly.getSurfacePressure() != null && !hourly.getSurfacePressure().isEmpty()) {
                surfacePressure = hourly.getSurfacePressure().get(0);
            }
        }

        return WeatherInfoDto.WeatherData.builder()
                .time(current.getTime())
                .temperature(current.getTemperature2m())
                .precipitation(current.getPrecipitation())
                .windSpeed(current.getWindSpeed10m())
                .windGust(current.getWindGusts10m())
                .windDirection(current.getWindDirection10m())
                .visibility(current.getVisibility())
                .cloudCover(current.getCloudCover())
                .weatherCode(current.getWeatherCode())
                .precipitationProbability(precipProb)
                .uvIndex(uvIndex)
                .relativeHumidity(relHumidity)
                .dewPoint(dewPoint)
                .surfacePressure(surfacePressure)
                .build();
    }

    @Override
    public WeatherInfoDto.MarineData getMarineByCoordinates(double latitude, double longitude) {
        OpenMeteoMarineResponse response = cacheService.getOrFetchMarine(
                latitude, longitude, () -> apiClient.fetchMarine(latitude, longitude));

        if (response == null) {
            return null;
        }

        if (response.getCurrent() != null) {
            OpenMeteoMarineResponse.CurrentData current = response.getCurrent();
            return WeatherInfoDto.MarineData.builder()
                    .time(current.getTime())
                    .waveHeight(current.getWaveHeight())
                    .waveDirection(current.getWaveDirection())
                    .wavePeriod(current.getWavePeriod())
                    .swellHeight(current.getSwellWaveHeight())
                    .swellDirection(current.getSwellWaveDirection())
                    .swellPeriod(current.getSwellWavePeriod())
                    .oceanCurrentVelocity(current.getOceanCurrentVelocity())
                    .oceanCurrentDirection(current.getOceanCurrentDirection())
                    .seaLevelHeight(current.getSeaLevelHeightMsl())
                    .seaSurfaceTemperature(current.getSeaSurfaceTemperature())
                    .build();
        }

        OpenMeteoMarineResponse.HourlyData hourly = response.getHourly();
        if (hourly == null || hourly.getTime() == null || hourly.getTime().isEmpty()) {
            return null;
        }

        return WeatherInfoDto.MarineData.builder()
                .time(hourly.getTime().get(0))
                .waveHeight(getFirstOrNull(hourly.getWaveHeight()))
                .waveDirection(getFirstOrNull(hourly.getWaveDirection()))
                .wavePeriod(getFirstOrNull(hourly.getWavePeriod()))
                .swellHeight(getFirstOrNull(hourly.getSwellWaveHeight()))
                .swellDirection(getFirstOrNull(hourly.getSwellWaveDirection()))
                .swellPeriod(getFirstOrNull(hourly.getSwellWavePeriod()))
                .oceanCurrentVelocity(getFirstOrNull(hourly.getOceanCurrentVelocity()))
                .oceanCurrentDirection(getFirstOrNull(hourly.getOceanCurrentDirection()))
                .seaLevelHeight(getFirstOrNull(hourly.getSeaLevelHeightMsl()))
                .seaSurfaceTemperature(getFirstOrNull(hourly.getSeaSurfaceTemperature()))
                .build();
    }

    @Override
    public WeatherInfoDto.TimeWindowForecast getTimeWindowForecast(double latitude, double longitude,
                                                                   LocalDate date,
                                                                   LocalTime startTime,
                                                                   LocalTime endTime) {
        OpenMeteoWeatherResponse weatherResp = cacheService.getOrFetchWeather(
                latitude, longitude, () -> apiClient.fetchWeather(latitude, longitude));
        OpenMeteoMarineResponse marineResp = cacheService.getOrFetchMarine(
                latitude, longitude, () -> apiClient.fetchMarine(latitude, longitude));

        LocalDateTime startWindow = LocalDateTime.of(date, startTime != null ? startTime : LocalTime.MIN);
        LocalDateTime endWindow;
        if (endTime != null && startTime != null && endTime.isBefore(startTime)) {
            endWindow = LocalDateTime.of(date.plusDays(1), endTime);
        } else {
            endWindow = LocalDateTime.of(date, endTime != null ? endTime : LocalTime.MAX);
        }

        Double peakWave = null;
        Double peakWind = null;
        Double peakGust = null;
        Double peakCurrent = null;
        Double minVis = null;
        Integer maxWeatherCode = null;
        double totalPrecip = 0.0;
        Double maxUv = null;
        double humiditySum = 0.0;
        int humidityCount = 0;
        Double maxCloud = null;

        // Process Weather Hourly
        if (weatherResp != null && weatherResp.getHourly() != null && weatherResp.getHourly().getTime() != null) {
            var hourly = weatherResp.getHourly();
            for (int i = 0; i < hourly.getTime().size(); i++) {
                try {
                    LocalDateTime time = LocalDateTime.parse(hourly.getTime().get(i));
                    if (!time.isBefore(startWindow) && !time.isAfter(endWindow)) {
                        if (hourly.getWindSpeed10m() != null && i < hourly.getWindSpeed10m().size() && hourly.getWindSpeed10m().get(i) != null) {
                            double w = hourly.getWindSpeed10m().get(i);
                            peakWind = peakWind == null ? w : Math.max(peakWind, w);
                        }
                        if (hourly.getWindGusts10m() != null && i < hourly.getWindGusts10m().size() && hourly.getWindGusts10m().get(i) != null) {
                            double g = hourly.getWindGusts10m().get(i);
                            peakGust = peakGust == null ? g : Math.max(peakGust, g);
                        }
                        if (hourly.getVisibility() != null && i < hourly.getVisibility().size() && hourly.getVisibility().get(i) != null) {
                            double v = hourly.getVisibility().get(i);
                            minVis = minVis == null ? v : Math.min(minVis, v);
                        }
                        if (hourly.getPrecipitation() != null && i < hourly.getPrecipitation().size() && hourly.getPrecipitation().get(i) != null) {
                            totalPrecip += hourly.getPrecipitation().get(i);
                        }
                        if (hourly.getWeatherCode() != null && i < hourly.getWeatherCode().size() && hourly.getWeatherCode().get(i) != null) {
                            int code = hourly.getWeatherCode().get(i);
                            if (code == 95 || code == 96 || code == 99) {
                                maxWeatherCode = code;
                            } else if (maxWeatherCode == null || (maxWeatherCode != 95 && maxWeatherCode != 96 && maxWeatherCode != 99 && code > maxWeatherCode)) {
                                maxWeatherCode = code;
                            }
                        }
                        if (hourly.getUvIndex() != null && i < hourly.getUvIndex().size() && hourly.getUvIndex().get(i) != null) {
                            double uv = hourly.getUvIndex().get(i);
                            maxUv = maxUv == null ? uv : Math.max(maxUv, uv);
                        }
                        if (hourly.getRelativeHumidity2m() != null && i < hourly.getRelativeHumidity2m().size() && hourly.getRelativeHumidity2m().get(i) != null) {
                            humiditySum += hourly.getRelativeHumidity2m().get(i);
                            humidityCount++;
                        }
                        if (hourly.getCloudCover() != null && i < hourly.getCloudCover().size() && hourly.getCloudCover().get(i) != null) {
                            double cc = hourly.getCloudCover().get(i).doubleValue();
                            maxCloud = maxCloud == null ? cc : Math.max(maxCloud, cc);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // Process Marine Hourly
        if (marineResp != null && marineResp.getHourly() != null && marineResp.getHourly().getTime() != null) {
            var hourly = marineResp.getHourly();
            for (int i = 0; i < hourly.getTime().size(); i++) {
                try {
                    LocalDateTime time = LocalDateTime.parse(hourly.getTime().get(i));
                    if (!time.isBefore(startWindow) && !time.isAfter(endWindow)) {
                        if (hourly.getWaveHeight() != null && i < hourly.getWaveHeight().size() && hourly.getWaveHeight().get(i) != null) {
                            double wh = hourly.getWaveHeight().get(i);
                            peakWave = peakWave == null ? wh : Math.max(peakWave, wh);
                        }
                        if (hourly.getOceanCurrentVelocity() != null && i < hourly.getOceanCurrentVelocity().size() && hourly.getOceanCurrentVelocity().get(i) != null) {
                            double c = hourly.getOceanCurrentVelocity().get(i);
                            peakCurrent = peakCurrent == null ? c : Math.max(peakCurrent, c);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return WeatherInfoDto.TimeWindowForecast.builder()
                .peakWaveHeight(peakWave)
                .peakWindSpeed(peakWind)
                .peakWindGust(peakGust)
                .peakOceanCurrent(peakCurrent)
                .minVisibility(minVis)
                .severeWeatherCode(maxWeatherCode)
                .totalPrecipitation(totalPrecip)
                .maxUvIndex(maxUv)
                .avgHumidity(humidityCount > 0 ? (double) Math.round(humiditySum / humidityCount) : null)
                .maxCloudCover(maxCloud)
                .build();
    }

    public void clearCache() {
        cacheService.clear();
    }

    public void invalidateCache(double latitude, double longitude) {
        cacheService.invalidate(latitude, longitude);
    }

    public boolean isWeatherCached(double latitude, double longitude) {
        return cacheService.isWeatherCached(latitude, longitude);
    }

    public boolean isMarineCached(double latitude, double longitude) {
        return cacheService.isMarineCached(latitude, longitude);
    }

    private <T> T getFirstOrNull(List<T> list) {
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }
}
