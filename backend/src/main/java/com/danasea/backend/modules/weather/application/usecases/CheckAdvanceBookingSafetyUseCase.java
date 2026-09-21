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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        this.categorySafetyRuleService = categorySafetyRuleService;
        this.weatherProviderPort = weatherProviderPort;
        this.weatherRuleEngine = weatherRuleEngine;
    }

    public CheckAdvanceBookingSafetyUseCase(
            CategorySafetyRuleService categorySafetyRuleService,
            WeatherProviderPort weatherProviderPort,
            WeatherRuleEngine weatherRuleEngine,
            JpaServiceSlotRepository slotRepository,
            JpaServiceRepository serviceRepository,
            JpaCategoryRepository categoryRepository) {
        this.categorySafetyRuleService = categorySafetyRuleService;
        this.weatherProviderPort = weatherProviderPort;
        this.weatherRuleEngine = weatherRuleEngine;
        this.slotRepository = slotRepository;
        this.serviceRepository = serviceRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Check safety for a specific booked slot by slotId.
     */
    public AdvanceBookingSafetyResponse checkBySlotId(UUID slotId) {
        if (slotRepository == null || serviceRepository == null) {
            throw new IllegalStateException("Slot and Service repositories are not available");
        }
        ServiceSlotJpaEntity slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot không tồn tại: " + slotId));

        ServiceJpaEntity service = serviceRepository.findById(slot.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Dịch vụ không tồn tại: " + slot.getServiceId()));

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
        LocalDate today = LocalDate.now();
        if (targetDate.isBefore(today)) {
            throw new IllegalArgumentException("Không thể kiểm tra an toàn cho ngày trong quá khứ: " + targetDate);
        }

        long daysAhead = ChronoUnit.DAYS.between(today, targetDate);
        if (daysAhead > 16) {
            throw new IllegalArgumentException("Vượt quá phạm vi dự báo tối đa của Open-Meteo (16 ngày): " + daysAhead + " ngày");
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
        return execute(request, LocalDate.now());
    }

    public AdvanceBookingSafetyResponse execute(AdvanceBookingSafetyRequest request, LocalDate referenceDate) {
        if (request == null || request.getBookingDate() == null) {
            throw new IllegalArgumentException("Ngày đặt chỗ (bookingDate) không được để trống");
        }

        LocalDate today = referenceDate != null ? referenceDate : LocalDate.now();
        long daysInAdvance = ChronoUnit.DAYS.between(today, request.getBookingDate());

        if (daysInAdvance < 0) {
            throw new IllegalArgumentException("Ngày đặt chỗ không thể là ngày trong quá khứ");
        }

        double lat = request.getLatitude() != null ? request.getLatitude() : DEFAULT_LATITUDE;
        double lon = request.getLongitude() != null ? request.getLongitude() : DEFAULT_LONGITUDE;
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
                    "Chưa có số liệu khí tượng hải văn chi tiết ngoài 16 ngày.",
                    "Hệ thống sẽ tự động cập nhật và gửi cảnh báo khi đơn hàng bước vào cửa sổ dự báo 16 ngày và các mốc T-24h, T-2h."
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
                    .warningMessage("LƯU Ý: Thời điểm đặt chỗ (" + daysInAdvance + " ngày tới) vượt quá giới hạn mô hình dự báo thời tiết 16 ngày của Open-Meteo.")
                    .summaryMessage("LƯU Ý: Thời điểm đặt chỗ (" + daysInAdvance + " ngày tới) vượt quá giới hạn mô hình dự báo thời tiết 16 ngày của Open-Meteo.")
                    .advisoryDetails(outOfRangeNotes)
                    .details(outOfRangeNotes)
                    .advisoryNotes(outOfRangeNotes)
                    .dataCoverage("OUT_OF_RANGE")
                    .dataCoverageNote("Dự báo thời tiết Open-Meteo hiện chỉ hỗ trợ tối đa 16 ngày. Đơn đặt trước " + daysInAdvance + " ngày nằm ngoài phạm vi mô hình dự báo trực tiếp.")
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
            dataCoverageNote = "Dữ liệu quan trắc kết hợp đầy đủ khí tượng (16 ngày) và hải văn (8 ngày) từ Open-Meteo.";
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

            dataCoverageNote = "Dự báo vượt mốc 8 ngày: Dữ liệu hải văn (sóng biển) được ước tính an toàn từ mô hình khí tượng 16 ngày của Open-Meteo kết hợp đánh giá các thông số gió, gió giật, mưa và dông sét.";
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

        List<String> details = new ArrayList<>(evalResult.getDetails() != null ? evalResult.getDetails() : List.of());
        if (estimatedMarine) {
            details.add(String.format("Lưu ý đặt trước %d ngày: Dữ liệu sóng biển là ước tính mô hình khí tượng. Hệ thống sẽ tự động đối soát hải văn thực tế tại mốc T-24h và T-2h.", daysInAdvance));
        }
        if (daysInAdvance >= 7 && daysInAdvance <= 14) {
            details.add(String.format("Khung thời gian đặt trước %d ngày: Khuyến nghị theo dõi diễn biến thời tiết cập nhật định kỳ trước ngày khởi hành.", daysInAdvance));
        }

        List<String> advisoryNotes = new ArrayList<>();
        if (marineCutoffExceeded) {
            advisoryNotes.add("LƯU Ý DỰ BÁO DÀI HẠN (9-14 ngày): Đánh giá an toàn dựa trên mô hình khí tượng (gió, dông bão, tầm nhìn). Dữ liệu sóng biển và dòng hải lưu chưa khả dụng do mô hình hải văn quốc tế giới hạn 8 ngày.");
            advisoryNotes.add("Dự báo sóng biển chi tiết sẽ được tự động kích hoạt 8 ngày trước giờ khởi hành.");
        } else {
            advisoryNotes.add("Dữ liệu khí tượng và hải văn đầy đủ (độ chính xác cao trong vòng 8 ngày).");
        }
        advisoryNotes.add("Hệ thống sẽ tiếp tục giám sát tự động qua Sliding Window tại mốc T-24h và T-2h trước giờ khởi hành.");

        String summary = evalResult.getWarningMessage();
        if (isProvisional && evalResult.isSafe()) {
            summary = "DỰ BÁO SƠ BỘ AN TOÀN: " + summary;
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
                .warningMessage(evalResult.getWarningMessage())
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
