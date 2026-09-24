package com.danasea.backend.modules.weather;

import com.danasea.backend.modules.weather.domain.models.CategorySafetyRule;
import com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WeatherRuleEngine Tests")
class WeatherRuleEngineTest {

    private WeatherRuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new WeatherRuleEngine();
    }

    @Test
    @DisplayName("✅ SUP & Kayak: Sóng 0.3m, gió 8 km/h -> GREEN")
    void evaluate_sup_idealConditions_returnsGreen() {
        CategorySafetyRule rule = CategorySafetyRule.getBySlug("cheo-sup-kayak");
        var result = ruleEngine.evaluate(rule, 0.3, 8.0, 10.0, 0.1, 5000.0, 0);

        assertTrue(result.isSafe());
        assertEquals(WeatherRuleEngine.ALERT_GREEN, result.getAlertLevel());
    }

    @Test
    @DisplayName("⚠️ SUP & Kayak: Sóng 0.6m -> YELLOW (Cảnh báo)")
    void evaluate_sup_moderateWave_returnsYellow() {
        CategorySafetyRule rule = CategorySafetyRule.getBySlug("cheo-sup-kayak");
        var result = ruleEngine.evaluate(rule, 0.6, 8.0, 10.0, 0.1, 5000.0, 0);

        assertTrue(result.isSafe());
        assertEquals(WeatherRuleEngine.ALERT_YELLOW, result.getAlertLevel());
        assertTrue(result.getWarningMessage().contains("CAUTION ALERT"));
    }

    @Test
    @DisplayName("❌ SUP & Kayak: Sóng 1.0m (vượt 0.8m) -> RED")
    void evaluate_sup_highWave_returnsRed() {
        CategorySafetyRule rule = CategorySafetyRule.getBySlug("cheo-sup-kayak");
        var result = ruleEngine.evaluate(rule, 1.0, 10.0, 15.0, 0.2, 5000.0, 0);

        assertFalse(result.isSafe());
        assertEquals(WeatherRuleEngine.ALERT_RED, result.getAlertLevel());
        assertTrue(result.getWarningMessage().contains("exceeds the maximum safe threshold"));
    }

    @Test
    @DisplayName("❌ Lặn ngắm san hô: Sóng 1.4m (vượt 1.2m) -> RED")
    void evaluate_diving_highWave_returnsRed() {
        CategorySafetyRule rule = CategorySafetyRule.getBySlug("lan-ngam-san-ho");
        var result = ruleEngine.evaluate(rule, 1.4, 10.0, 15.0, 0.2, 5000.0, 0);

        assertFalse(result.isSafe());
        assertEquals(WeatherRuleEngine.ALERT_RED, result.getAlertLevel());
    }

    @Test
    @DisplayName("❌ Lặn ngắm san hô: Dòng hải lưu mạnh 0.6 m/s (vượt 0.5 m/s) -> RED")
    void evaluate_diving_strongCurrent_returnsRed() {
        CategorySafetyRule rule = CategorySafetyRule.getBySlug("lan-ngam-san-ho");
        var result = ruleEngine.evaluate(rule, 0.5, 10.0, 15.0, 0.6, 5000.0, 0);

        assertFalse(result.isSafe());
        assertEquals(WeatherRuleEngine.ALERT_RED, result.getAlertLevel());
        assertTrue(result.getWarningMessage().contains("Ocean-current speed"));
    }

    @Test
    @DisplayName("✅ Dù bay biển (Parasailing): Sóng 0.5m, gió 18 km/h, giật 22 km/h -> GREEN")
    void evaluate_parasailing_goodConditions_returnsGreen() {
        CategorySafetyRule rule = CategorySafetyRule.getBySlug("cano-du-bay");
        var result = ruleEngine.evaluate(rule, 0.5, 18.0, 22.0, 0.2, 5000.0, 1);

        assertTrue(result.isSafe());
        assertEquals(WeatherRuleEngine.ALERT_GREEN, result.getAlertLevel());
    }

    @Test
    @DisplayName("❌ Dù bay biển (Parasailing): Gió giật 39 km/h (vượt chuẩn ASTM F3099 37 km/h) -> RED")
    void evaluate_parasailing_highGust_returnsRed() {
        CategorySafetyRule rule = CategorySafetyRule.getBySlug("cano-du-bay");
        var result = ruleEngine.evaluate(rule, 0.5, 20.0, 39.0, 0.2, 5000.0, 1);

        assertFalse(result.isSafe());
        assertEquals(WeatherRuleEngine.ALERT_RED, result.getAlertLevel());
        assertTrue(result.getWarningMessage().contains("Peak wind gust"));
    }

    @Test
    @DisplayName("❌ Sấm sét giông bão (WMO Code 95) -> RED tuyệt đối cho mọi trò chơi")
    void evaluate_thunderstorm_alwaysRed() {
        CategorySafetyRule supRule = CategorySafetyRule.getBySlug("cheo-sup-kayak");
        CategorySafetyRule diveRule = CategorySafetyRule.getBySlug("lan-ngam-san-ho");
        CategorySafetyRule yachtRule = CategorySafetyRule.getBySlug("du-thuyen-ngam-hoang-hon");

        // Ngay cả khi sóng biển rất phẳng lặng 0.2m và không có gió
        var res1 = ruleEngine.evaluate(supRule, 0.2, 5.0, 5.0, 0.1, 10000.0, 95);
        var res2 = ruleEngine.evaluate(diveRule, 0.2, 5.0, 5.0, 0.1, 10000.0, 95);
        var res3 = ruleEngine.evaluate(yachtRule, 0.2, 5.0, 5.0, 0.1, 10000.0, 99);

        assertFalse(res1.isSafe());
        assertEquals(WeatherRuleEngine.ALERT_RED, res1.getAlertLevel());

        assertFalse(res2.isSafe());
        assertEquals(WeatherRuleEngine.ALERT_RED, res2.getAlertLevel());

        assertFalse(res3.isSafe());
        assertEquals(WeatherRuleEngine.ALERT_RED, res3.getAlertLevel());
    }
}
