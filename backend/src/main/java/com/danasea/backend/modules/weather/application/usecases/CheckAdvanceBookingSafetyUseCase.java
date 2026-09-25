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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.i18n.LocaleContextHolder;
import com.danasea.backend.modules.weather.application.services.WeatherSafetyMessageRenderer;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.i18n.SupportedLanguage;

/**
 * UseCase assessing marine safety for reservations 7 to 14 days (up to 16 days) in advance.
 * Features:
 * - Multi-day time-window metric extraction for target booking date & slot time.
 * - Full marine + meteorology evaluation within Open-Meteo Marine API range (<= 8 days).
 * - Safe meteorological estimation & evaluation beyond day 8 (days 9-16) when Marine API data is unavailable.
 * - Out-of-range boundary safety handling beyond day 16.
 */
@Slf4j
@Service
public class CheckAdvanceBookingSafetyUseCase {

    private final CategorySafetyRuleService categorySafetyRuleService;
    private final WeatherProviderPort weatherProviderPort;
    private final WeatherRuleEngine weatherRuleEngine;
    private final Clock clock;
    private WeatherSafetyMessageRenderer weatherMessages = new WeatherSafetyMessageRenderer();

    @Autowired
    void setLocalizedMessageService(LocalizedMessageService messages) {
        this.weatherMessages = new WeatherSafetyMessageRenderer(messages);
    }

    @Autowired(required = false)
    private JpaServiceSlotRepository slotRepository;

    @Autowired(required = false)
    private JpaServiceRepository serviceRepository;

    @Autowired(required = false)
    private JpaCategoryRepository categoryRepository;

    public static final double DEFAULT_LATITUDE = 16.089035780716284;
    public static final double DEFAULT_LONGITUDE = 108.24959555394304;

    public CheckAdvanceBookingSafetyUseCase(
            CategorySafetyRuleService categorySafetyRuleService,
            WeatherProviderPort weatherProviderPort,
            WeatherRuleEngine weatherRuleEngine) {
        this(categorySafetyRuleService, weatherProviderPort, weatherRuleEngine, null, null, null,
                Clock.systemDefaultZone());
    }

    public CheckAdvanceBookingSafetyUseCase(
            CategorySafetyRuleService categorySafetyRuleService,
            WeatherProviderPort weatherProviderPort,
            WeatherRuleEngine weatherRuleEngine,
            JpaServiceSlotRepository slotRepository,
            JpaServiceRepository serviceRepository,
            JpaCategoryRepository categoryRepository) {
        this(categorySafetyRuleService, weatherProviderPort, weatherRuleEngine,
                slotRepository, serviceRepository, categoryRepository, Clock.systemDefaultZone());
    }

    @Autowired
    public CheckAdvanceBookingSafetyUseCase(
            CategorySafetyRuleService categorySafetyRuleService,
            WeatherProviderPort weatherProviderPort,
            WeatherRuleEngine weatherRuleEngine,
            @Autowired(required = false) JpaServiceSlotRepository slotRepository,
            @Autowired(required = false) JpaServiceRepository serviceRepository,
            @Autowired(required = false) JpaCategoryRepository categoryRepository,
            Clock clock) {
        this.categorySafetyRuleService = categorySafetyRuleService;
        this.weatherProviderPort = weatherProviderPort;
        this.weatherRuleEngine = weatherRuleEngine;
        this.slotRepository = slotRepository;
        this.serviceRepository = serviceRepository;
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    /**
     * Check safety for a specific booked slot by slotId.
     */
    public AdvanceBookingSafetyResponse checkBySlotId(UUID slotId) {
        if (slotRepository == null || serviceRepository == null) {
            throw new IllegalStateException("Slot and Service repositories are not available");
        }
        ServiceSlotJpaEntity slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Service slot not found: " + slotId));

        ServiceJpaEntity service = serviceRepository.findById(slot.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + slot.getServiceId()));

        String categorySlug = "default";
        if (service.getCategoryId() != null && categoryRepository != null) {
            categorySlug = categoryRepository.findById(service.getCategoryId())
                    .map(CategoryJpaEntity::getSlug)
                    .orElse("default");
        }

        double lat = service.getLatitude() != null ? service.getLatitude().doubleValue() : DEFAULT_LATITUDE;
        double lng = service.getLongitude() != null ? service.getLongitude().doubleValue() : DEFAULT_LONGITUDE;

        return checkSafety(slotId, service.getId(), categorySlug, slot.getDate(), slot.getStartTime(), slot.getEndTime(), lat, lng);
    }

    /**
     * Check safety by explicit parameters with strict boundary checks.
     */
    public AdvanceBookingSafetyResponse checkSafety(UUID slotId, UUID serviceId, String categorySlug,
                                                   LocalDate targetDate, LocalTime startTime, LocalTime endTime,
                                                   double latitude, double longitude) {
        LocalDate today = LocalDate.now(clock);
        if (targetDate.isBefore(today)) {
            throw new IllegalArgumentException("Safety cannot be checked for a past date: " + targetDate);
        }

        long daysAhead = ChronoUnit.DAYS.between(today, targetDate);
        if (daysAhead > 16) {
            throw new IllegalArgumentException("The date exceeds the 16-day Open-Meteo forecast range: " + daysAhead + " days");
        }

        AdvanceBookingSafetyRequest req = AdvanceBookingSafetyRequest.builder()
                .categorySlug(categorySlug)
                .latitude(latitude)
                .longitude(longitude)
                .bookingDate(targetDate)
                .startTime(startTime != null ? startTime : LocalTime.of(8, 0))
                .endTime(endTime != null ? endTime : LocalTime.of(17, 0))
                .build();

        AdvanceBookingSafetyResponse res = execute(req, today);
        res.setSlotId(slotId);
        res.setServiceId(serviceId);
        return res;
    }

    public AdvanceBookingSafetyResponse execute(AdvanceBookingSafetyRequest request) {
        return execute(request, LocalDate.now(clock));
    }

    public AdvanceBookingSafetyResponse execute(AdvanceBookingSafetyRequest request, LocalDate referenceDate) {
        if (request == null || request.getBookingDate() == null) {
            throw new IllegalArgumentException("bookingDate is required");
        }

        LocalDate today = referenceDate != null ? referenceDate : LocalDate.now(clock);
        long daysInAdvance = ChronoUnit.DAYS.between(today, request.getBookingDate());

        if (daysInAdvance < 0) {
            throw new IllegalArgumentException("bookingDate cannot be in the past");
        }

        double lat = request.getLatitude() != null ? request.getLatitude() : DEFAULT_LATITUDE;
        double lon = request.getLongitude() != null ? request.getLongitude() : DEFAULT_LONGITUDE;
        if (!Double.isFinite(lat) || lat < -90 || lat > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (!Double.isFinite(lon) || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
        LocalTime startTime = request.getStartTime() != null ? request.getStartTime() : LocalTime.of(8, 0);
        LocalTime endTime = request.getEndTime() != null ? request.getEndTime() : LocalTime.of(17, 0);

        // Resolve Category Safety Rule
        CategorySafetyRule rule;
        if (request.getCategoryId() != null && (request.getCategorySlug() == null || request.getCategorySlug().isBlank())) {
            try {
                rule = categorySafetyRuleService.getRuleByCategoryId(request.getCategoryId());
            } catch (Exception e) {
                rule = categorySafetyRuleService.getRuleByCategorySlug("default");
            }
        } else {
            String slug = request.getCategorySlug() != null ? request.getCategorySlug().trim() : "default";
            rule = categorySafetyRuleService.getRuleByCategorySlug(slug);
        }

        if (rule == null) {
            rule = CategorySafetyRule.DEFAULT_RULE;
        }

        // Boundary check: beyond 16 days returns OUT_OF_RANGE
        if (daysInAdvance > 16) {
            List<String> outOfRangeNotes = List.of(
                    "Detailed marine and weather data is unavailable beyond 16 days.",
                    "The system will update the assessment when the booking enters the 16-day window and at T-24h and T-2h."
            );
            return AdvanceBookingSafetyResponse.builder()
                    .categorySlug(rule.getCategorySlug())
                    .categoryName(rule.getCategoryName())
                    .bookingDate(request.getBookingDate())
                    .startTime(startTime)
                    .endTime(endTime)
                    .daysInAdvance(daysInAdvance)
                    .daysAhead((int) daysInAdvance)
                    .isSafe(true)
                    .alertLevel(WeatherRuleEngine.ALERT_YELLOW)
                    .safetyStatus(WeatherRuleEngine.ALERT_YELLOW)
                    .isProvisional(true)
                    .marineCutoffExceeded(true)
                    .warningMessage("NOTICE: The booking date (" + daysInAdvance + " days ahead) exceeds Open-Meteo's 16-day forecast window.")
                    .summaryMessage("NOTICE: The booking date (" + daysInAdvance + " days ahead) exceeds Open-Meteo's 16-day forecast window.")
                    .advisoryDetails(outOfRangeNotes)
                    .details(outOfRangeNotes)
                    .advisoryNotes(outOfRangeNotes)
                    .dataCoverage("OUT_OF_RANGE")
                    .dataCoverageNote("Open-Meteo currently provides forecasts for up to 16 days. This booking is outside the direct forecast window.")
                    .estimatedMarine(true)
                    .confidenceLevel("LOW")
                    .forecast(null)
                    .build();
        }

        // Extract time-window metrics
        WeatherInfoDto.TimeWindowForecast forecast = weatherProviderPort.getTimeWindowForecast(
                lat, lon, request.getBookingDate(), startTime, endTime
        );

        if (forecast == null) {
            forecast = WeatherInfoDto.TimeWindowForecast.builder().build();
        }

        Double peakWave = forecast.getPeakWaveHeight();
        Double peakWind = forecast.getPeakWindSpeed();
        Double peakGust = forecast.getPeakWindGust();
        Double peakCurrent = forecast.getPeakOceanCurrent();
        Double minVis = forecast.getMinVisibility();
        Integer severeWeatherCode = forecast.getSevereWeatherCode();

        boolean marineCutoffExceeded = daysInAdvance > 8;
        boolean isProvisional = marineCutoffExceeded;
        boolean estimatedMarine = false;
        String dataCoverage;
        String dataCoverageNote;
        String confidenceLevel;

        if (!marineCutoffExceeded && peakWave != null) {
            dataCoverage = "FULL_MARINE_AND_METEOROLOGY";
            dataCoverageNote = "Full Open-Meteo weather (16 days) and marine (8 days) forecast coverage is available.";
            confidenceLevel = daysInAdvance <= 3 ? "HIGH" : "MEDIUM";
        } else {
            // Marine API limit is 8 days; safely estimate wave height based on wind speed
            estimatedMarine = true;
            dataCoverage = "METEOROLOGY_ESTIMATED_MARINE";
            confidenceLevel = "ADVISORY";

            if (peakWind != null && peakWave == null) {
                // Coastal empirical wind-wave relation: Hs ≈ 0.024 * (V_kmh)^1.15
                peakWave = Math.round((0.024 * Math.pow(peakWind, 1.15)) * 100.0) / 100.0;
                forecast.setPeakWaveHeight(peakWave);
            }

            dataCoverageNote = "Beyond 8 days, marine conditions are conservatively estimated from Open-Meteo weather data, including wind, gusts, rain, and thunderstorms.";
        }

        // Evaluate using rule engine
        WeatherRuleEngine.SafetyEvaluationResult evalResult = weatherRuleEngine.evaluate(
                rule,
                peakWave,
                peakWind,
                peakGust,
                !marineCutoffExceeded ? peakCurrent : null,
                minVis,
                severeWeatherCode
        );

        SupportedLanguage language = LocaleContextHolder.getLocaleContext() == null
                ? SupportedLanguage.EN
                : SupportedLanguage.fromTag(LocaleContextHolder.getLocale().toLanguageTag())
                        .orElse(SupportedLanguage.VI);
        List<String> details = new ArrayList<>(weatherMessages.renderDetails(evalResult, language));
        if (estimatedMarine) {
            details.add(language == SupportedLanguage.EN
                    ? String.format("For this booking %d days ahead, wave data is model-estimated. The system will reassess at T-24h and T-2h.", daysInAdvance)
                    : String.format("Lưu ý đặt trước %d ngày: Dữ liệu sóng biển là ước tính mô hình khí tượng. Hệ thống sẽ tự động đối soát hải văn thực tế tại mốc T-24h và T-2h.", daysInAdvance));
        }
        if (daysInAdvance >= 7 && daysInAdvance <= 14) {
            details.add(language == SupportedLanguage.EN
                    ? String.format("This booking is %d days ahead. Review updated forecasts before departure.", daysInAdvance)
                    : String.format("Khung thời gian đặt trước %d ngày: Khuyến nghị theo dõi diễn biến thời tiết cập nhật định kỳ trước ngày khởi hành.", daysInAdvance));
        }

        List<String> advisoryNotes = new ArrayList<>();
        if (marineCutoffExceeded) {
            advisoryNotes.add(language == SupportedLanguage.EN
                    ? "LONG-RANGE NOTICE (9-14 days): the safety assessment uses weather data; detailed waves and currents are unavailable beyond the 8-day marine forecast window."
                    : "LƯU Ý DỰ BÁO DÀI HẠN (9-14 ngày): Đánh giá an toàn dựa trên mô hình khí tượng (gió, dông bão, tầm nhìn). Dữ liệu sóng biển và dòng hải lưu chưa khả dụng do mô hình hải văn quốc tế giới hạn 8 ngày.");
            advisoryNotes.add(language == SupportedLanguage.EN
                    ? "Detailed wave forecasting will begin automatically 8 days before departure."
                    : "Dự báo sóng biển chi tiết sẽ được tự động kích hoạt 8 ngày trước giờ khởi hành.");
        } else {
            advisoryNotes.add(language == SupportedLanguage.EN
                    ? "Full weather and marine forecast data is available within 8 days."
                    : "Dữ liệu khí tượng và hải văn đầy đủ (độ chính xác cao trong vòng 8 ngày).");
        }
        advisoryNotes.add(language == SupportedLanguage.EN
                ? "The system will continue monitoring at T-24h and T-2h before departure."
                : "Hệ thống sẽ tiếp tục giám sát tự động qua Sliding Window tại mốc T-24h và T-2h trước giờ khởi hành.");

        String localizedWarning = weatherMessages.render(evalResult, language);
        String summary = localizedWarning;
        if (isProvisional && evalResult.isSafe()) {
            summary = (language == SupportedLanguage.EN
                    ? "PROVISIONALLY SAFE FORECAST: "
                    : "DỰ BÁO SƠ BỘ AN TOÀN: ") + summary;
        }

        return AdvanceBookingSafetyResponse.builder()
                .isSafe(evalResult.isSafe())
                .alertLevel(evalResult.getAlertLevel())
                .safetyStatus(evalResult.getAlertLevel())
                .isProvisional(isProvisional)
                .marineCutoffExceeded(marineCutoffExceeded)
                .categorySlug(rule.getCategorySlug())
                .categoryName(rule.getCategoryName())
                .bookingDate(request.getBookingDate())
                .startTime(startTime)
                .endTime(endTime)
                .daysInAdvance(daysInAdvance)
                .daysAhead((int) daysInAdvance)
                .peakWaveHeightM(!marineCutoffExceeded ? forecast.getPeakWaveHeight() : null)
                .peakWindSpeedKmh(forecast.getPeakWindSpeed())
                .peakWindGustKmh(forecast.getPeakWindGust())
                .peakOceanCurrentMs(!marineCutoffExceeded ? forecast.getPeakOceanCurrent() : null)
                .minVisibilityM(forecast.getMinVisibility())
                .severeWeatherCode(forecast.getSevereWeatherCode())
                .totalPrecipitationMm(forecast.getTotalPrecipitation() != null ? forecast.getTotalPrecipitation() : 0.0)
                .cautionWaveHeightM(rule.getCautionWaveHeightM())
                .maxWaveHeightM(rule.getMaxWaveHeightM())
                .cautionWindSpeedKmh(rule.getCautionWindSpeedKmh())
                .maxWindSpeedKmh(rule.getMaxWindSpeedKmh())
                .maxWindGustKmh(rule.getMaxWindGustKmh())
                .maxOceanCurrentMs(rule.getMaxOceanCurrentMs())
                .ruleMinVisibilityM(rule.getMinVisibilityM())
                .warningMessage(localizedWarning)
                .summaryMessage(summary)
                .advisoryDetails(details)
                .details(details)
                .advisoryNotes(advisoryNotes)
                .dataCoverage(dataCoverage)
                .dataCoverageNote(dataCoverageNote)
                .estimatedMarine(estimatedMarine)
                .confidenceLevel(confidenceLevel)
                .forecast(forecast)
                .build();
    }
}
