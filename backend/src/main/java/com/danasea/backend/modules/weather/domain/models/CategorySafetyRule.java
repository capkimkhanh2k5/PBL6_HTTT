package com.danasea.backend.modules.weather.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySafetyRule implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID categoryId;
    private String categorySlug;
    private String categoryName;
    private Double maxWaveHeightM;        // Ngưỡng sóng đỏ (m)
    private Double cautionWaveHeightM;    // Ngưỡng sóng vàng (m)
    private Double maxWindSpeedKmh;       // Ngưỡng gió duy trì đỏ (km/h)
    private Double cautionWindSpeedKmh;   // Ngưỡng gió duy trì vàng (km/h)
    private Double maxWindGustKmh;        // Ngưỡng gió giật đỏ (km/h)
    private Double maxOceanCurrentMs;     // Ngưỡng dòng chảy hải lưu đỏ (m/s)
    private Double minVisibilityM;        // Tầm nhìn tối thiểu (m)

    @Builder.Default
    private String fatalThunderstormCodes = "95,96,99";

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Các mã WMO chỉ sấm sét giông bão cấm 100% mọi hoạt động
    public static final Set<Integer> FATAL_THUNDERSTORM_CODES = Set.of(95, 96, 99);

    public static final Map<String, CategorySafetyRule> REGISTRY = new HashMap<>();

    static {
        // 1. Chèo SUP & Kayak (nhẹ, dễ lật, rủi ro bị gió đẩy xa bờ)
        REGISTRY.put("cheo-sup-kayak", CategorySafetyRule.builder()
                .categorySlug("cheo-sup-kayak")
                .categoryName("Chèo SUP & Kayak")
                .cautionWaveHeightM(0.5)
                .maxWaveHeightM(0.8)
                .cautionWindSpeedKmh(12.0)
                .maxWindSpeedKmh(20.0)
                .maxWindGustKmh(28.0)
                .maxOceanCurrentMs(0.3)
                .minVisibilityM(2000.0)
                .build());

        // 2. Lặn ngắm san hô & Đi bộ dưới biển (cần nước trong, sóng êm, không dòng xoáy)
        REGISTRY.put("lan-ngam-san-ho", CategorySafetyRule.builder()
                .categorySlug("lan-ngam-san-ho")
                .categoryName("Lặn ngắm san hô & Đi bộ dưới biển")
                .cautionWaveHeightM(0.8)
                .maxWaveHeightM(1.2)
                .cautionWindSpeedKmh(15.0)
                .maxWindSpeedKmh(25.0)
                .maxWindGustKmh(35.0)
                .maxOceanCurrentMs(0.5)
                .minVisibilityM(3000.0)
                .build());

        // 3. Cano lướt sóng & Dù bay biển (Parasailing - cực kỳ nguy hiểm với gió giật theo chuẩn ASTM F3099)
        REGISTRY.put("cano-du-bay", CategorySafetyRule.builder()
                .categorySlug("cano-du-bay")
                .categoryName("Cano lướt sóng & Dù bay biển")
                .cautionWaveHeightM(0.7)
                .maxWaveHeightM(1.0)
                .cautionWindSpeedKmh(20.0)
                .maxWindSpeedKmh(28.0)
                .maxWindGustKmh(37.0) // 20 knots giới hạn chuẩn ASTM F3099
                .maxOceanCurrentMs(0.8)
                .minVisibilityM(1800.0)
                .build());

        // 4. Mô tô nước (Jetski) (tốc độ cao, nguy cơ lật úp khi sóng lớn)
        REGISTRY.put("mo-to-nuoc-jetski", CategorySafetyRule.builder()
                .categorySlug("mo-to-nuoc-jetski")
                .categoryName("Mô tô nước (Jetski)")
                .cautionWaveHeightM(0.6)
                .maxWaveHeightM(1.2)
                .cautionWindSpeedKmh(20.0)
                .maxWindSpeedKmh(30.0)
                .maxWindGustKmh(40.0)
                .maxOceanCurrentMs(0.6)
                .minVisibilityM(1500.0)
                .build());

        // 5. Trượt phao chuối (Banana Boat - dễ văng hành khách)
        REGISTRY.put("truot-phao-chuoi", CategorySafetyRule.builder()
                .categorySlug("truot-phao-chuoi")
                .categoryName("Trượt phao chuối cảm giác mạnh")
                .cautionWaveHeightM(0.5)
                .maxWaveHeightM(1.0)
                .cautionWindSpeedKmh(18.0)
                .maxWindSpeedKmh(25.0)
                .maxWindGustKmh(35.0)
                .maxOceanCurrentMs(0.5)
                .minVisibilityM(2000.0)
                .build());

        // 6. Du thuyền ngắm hoàng hôn (Catamaran / Yacht - tàu vỏ đôi ổn định hơn)
        REGISTRY.put("du-thuyen-ngam-hoang-hon", CategorySafetyRule.builder()
                .categorySlug("du-thuyen-ngam-hoang-hon")
                .categoryName("Du thuyền ngắm hoàng hôn vịnh")
                .cautionWaveHeightM(0.8)
                .maxWaveHeightM(1.5)
                .cautionWindSpeedKmh(25.0)
                .maxWindSpeedKmh(38.0) // Gió cấp 5-6 Beaufort
                .maxWindGustKmh(48.0)
                .maxOceanCurrentMs(0.7)
                .minVisibilityM(2000.0)
                .build());
    }

    // Baseline mặc định an toàn nếu không khớp danh mục nào
    public static final CategorySafetyRule DEFAULT_RULE = CategorySafetyRule.builder()
            .categorySlug("default")
            .categoryName("Hoạt động biển tổng quát")
            .cautionWaveHeightM(0.7)
            .maxWaveHeightM(1.2)
            .cautionWindSpeedKmh(18.0)
            .maxWindSpeedKmh(28.0)
            .maxWindGustKmh(38.0)
            .maxOceanCurrentMs(0.5)
            .minVisibilityM(2000.0)
            .build();

    public static CategorySafetyRule getBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return DEFAULT_RULE;
        }
        return REGISTRY.getOrDefault(slug.toLowerCase().trim(), DEFAULT_RULE);
    }
}
