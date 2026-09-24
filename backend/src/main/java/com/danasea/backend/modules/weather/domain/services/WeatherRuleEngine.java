package com.danasea.backend.modules.weather.domain.services;

import com.danasea.backend.modules.weather.domain.models.CategorySafetyRule;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WeatherRuleEngine {

    public static final String ALERT_GREEN = "GREEN";
    public static final String ALERT_YELLOW = "YELLOW";
    public static final String ALERT_RED = "RED";

    @Data
    @Builder
    public static class SafetyEvaluationResult {
        private boolean isSafe;
        private String alertLevel;
        private String warningMessage;
        private List<String> details;
        private List<SafetyFinding> findings;
    }

    public record SafetyFinding(String code, Object[] args) {
        public SafetyFinding {
            args = args == null ? new Object[0] : args.clone();
        }

        @Override
        public Object[] args() {
            return args.clone();
        }
    }

    public SafetyEvaluationResult evaluate(CategorySafetyRule rule,
                                           Double waveHeight,
                                           Double windSpeed,
                                           Double windGust,
                                           Double oceanCurrent,
                                           Double visibility,
                                           Integer weatherCode) {
        if (rule == null) {
            rule = CategorySafetyRule.getBySlug("default");
        }

        List<String> redViolations = new ArrayList<>();
        List<String> yellowWarnings = new ArrayList<>();
        List<SafetyFinding> redFindings = new ArrayList<>();
        List<SafetyFinding> yellowFindings = new ArrayList<>();

        // 1. Kiểm tra giông bão sấm sét toàn cục (Global Fatal Conditions)
        if (weatherCode != null && CategorySafetyRule.FATAL_THUNDERSTORM_CODES.contains(weatherCode)) {
            redViolations.add("Phát hiện dông sét hoặc mưa rào sấm chớp nguy hiểm (Mã WMO: " + weatherCode + ")");
            redFindings.add(new SafetyFinding("thunderstorm", new Object[]{weatherCode}));
        }

        // 2. Kiểm tra chiều cao sóng biển (Wave Height)
        if (waveHeight != null) {
            if (rule.getMaxWaveHeightM() != null && waveHeight > rule.getMaxWaveHeightM()) {
                redViolations.add(String.format("Chiều cao sóng biển %.1fm vượt ngưỡng an toàn tối đa (%.1fm) của danh mục %s",
                        waveHeight, rule.getMaxWaveHeightM(), rule.getCategoryName()));
                redFindings.add(new SafetyFinding("wave.red", new Object[]{waveHeight, rule.getMaxWaveHeightM()}));
            } else if (rule.getCautionWaveHeightM() != null && waveHeight > rule.getCautionWaveHeightM()) {
                yellowWarnings.add(String.format("Chiều cao sóng biển %.1fm ở mức cảnh báo thận trọng (> %.1fm)",
                        waveHeight, rule.getCautionWaveHeightM()));
                yellowFindings.add(new SafetyFinding("wave.yellow", new Object[]{waveHeight, rule.getCautionWaveHeightM()}));
            }
        }

        // 3. Kiểm tra gió duy trì (Wind Speed)
        if (windSpeed != null) {
            if (rule.getMaxWindSpeedKmh() != null && windSpeed > rule.getMaxWindSpeedKmh()) {
                redViolations.add(String.format("Tốc độ gió duy trì %.1f km/h vượt trần cho phép (%.1f km/h)",
                        windSpeed, rule.getMaxWindSpeedKmh()));
                redFindings.add(new SafetyFinding("wind.red", new Object[]{windSpeed, rule.getMaxWindSpeedKmh()}));
            } else if (rule.getCautionWindSpeedKmh() != null && windSpeed > rule.getCautionWindSpeedKmh()) {
                yellowWarnings.add(String.format("Tốc độ gió duy trì %.1f km/h ở mức cảnh báo (> %.1f km/h)",
                        windSpeed, rule.getCautionWindSpeedKmh()));
                yellowFindings.add(new SafetyFinding("wind.yellow", new Object[]{windSpeed, rule.getCautionWindSpeedKmh()}));
            }
        }

        // 4. Kiểm tra gió giật (Wind Gusts) - Cực kỳ quan trọng cho dù lượn và tàu cano
        if (windGust != null && rule.getMaxWindGustKmh() != null && windGust > rule.getMaxWindGustKmh()) {
            redViolations.add(String.format("Gió giật cực đại %.1f km/h vượt trần khí động học an toàn (%.1f km/h)",
                    windGust, rule.getMaxWindGustKmh()));
            redFindings.add(new SafetyFinding("gust.red", new Object[]{windGust, rule.getMaxWindGustKmh()}));
        }

        // 5. Kiểm tra dòng chảy hải lưu (Ocean Current)
        if (oceanCurrent != null && rule.getMaxOceanCurrentMs() != null && oceanCurrent > rule.getMaxOceanCurrentMs()) {
            redViolations.add(String.format("Vận tốc dòng hải lưu %.2f m/s vượt ngưỡng an toàn (%.2f m/s), nguy cơ trôi dạt",
                    oceanCurrent, rule.getMaxOceanCurrentMs()));
            redFindings.add(new SafetyFinding("current.red", new Object[]{oceanCurrent, rule.getMaxOceanCurrentMs()}));
        }

        // 6. Kiểm tra tầm nhìn (Visibility)
        if (visibility != null && rule.getMinVisibilityM() != null && visibility < rule.getMinVisibilityM()) {
            redViolations.add(String.format("Tầm nhìn ngang trên biển chỉ đạt %.0fm, thấp hơn mức tối thiểu (%.0fm) do sương mù hoặc mưa lớn",
                    visibility, rule.getMinVisibilityM()));
            redFindings.add(new SafetyFinding("visibility.red", new Object[]{visibility, rule.getMinVisibilityM()}));
        }

        // Tổng hợp kết luận
        if (!redViolations.isEmpty()) {
            return SafetyEvaluationResult.builder()
                    .isSafe(false)
                    .alertLevel(ALERT_RED)
                    .warningMessage("CẢNH BÁO NGUY HIỂM: " + String.join("; ", redViolations))
                    .details(redViolations)
                    .findings(redFindings)
                    .build();
        }

        if (!yellowWarnings.isEmpty()) {
            return SafetyEvaluationResult.builder()
                    .isSafe(true)
                    .alertLevel(ALERT_YELLOW)
                    .warningMessage("CẢNH BÁO THẬN TRỌNG: " + String.join("; ", yellowWarnings) + ". Khuyến cáo trang bị bảo hộ nghiêm ngặt.")
                    .details(yellowWarnings)
                    .findings(yellowFindings)
                    .build();
        }

        return SafetyEvaluationResult.builder()
                .isSafe(true)
                .alertLevel(ALERT_GREEN)
                .warningMessage(String.format("Điều kiện hải văn và thời tiết lý tưởng cho hoạt động %s.", rule.getCategoryName()))
                .details(List.of("Thời tiết tốt, an toàn xuất bến."))
                .findings(List.of(new SafetyFinding("safe", new Object[0])))
                .build();
    }
}
