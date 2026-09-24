-- V6__create_category_safety_rules_table.sql
-- Create category_safety_rules table, seed benchmark rules, and upgrade safety_rule_evaluations

CREATE TABLE IF NOT EXISTS category_safety_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID UNIQUE REFERENCES categories(id) ON DELETE CASCADE,
    category_slug VARCHAR(120) NOT NULL UNIQUE,
    category_name VARCHAR(255) NOT NULL,
    caution_wave_height_m NUMERIC(5,2),
    max_wave_height_m NUMERIC(5,2) NOT NULL,
    caution_wind_speed_kmh NUMERIC(5,2),
    max_wind_speed_kmh NUMERIC(5,2) NOT NULL,
    max_wind_gust_kmh NUMERIC(5,2),
    max_ocean_current_ms NUMERIC(5,2),
    min_visibility_m NUMERIC(7,2),
    fatal_thunderstorm_codes VARCHAR(255) DEFAULT '95,96,99',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_wave_thresholds CHECK (caution_wave_height_m IS NULL OR max_wave_height_m IS NULL OR caution_wave_height_m <= max_wave_height_m),
    CONSTRAINT chk_wind_thresholds CHECK (caution_wind_speed_kmh IS NULL OR max_wind_speed_kmh IS NULL OR caution_wind_speed_kmh <= max_wind_speed_kmh),
    CONSTRAINT chk_positive_thresholds CHECK (
        (caution_wave_height_m IS NULL OR caution_wave_height_m >= 0) AND
        (max_wave_height_m IS NULL OR max_wave_height_m >= 0) AND
        (caution_wind_speed_kmh IS NULL OR caution_wind_speed_kmh >= 0) AND
        (max_wind_speed_kmh IS NULL OR max_wind_speed_kmh >= 0) AND
        (max_wind_gust_kmh IS NULL OR max_wind_gust_kmh >= 0) AND
        (max_ocean_current_ms IS NULL OR max_ocean_current_ms >= 0) AND
        (min_visibility_m IS NULL OR min_visibility_m >= 0)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_category_safety_rules_slug ON category_safety_rules(category_slug);
CREATE UNIQUE INDEX IF NOT EXISTS idx_category_safety_rules_category_id ON category_safety_rules(category_id) WHERE category_id IS NOT NULL;

-- 1. Ensure the 6 core categories exist in categories table
INSERT INTO categories (id, name, name_en, slug, parent_id, icon_url, is_active, requires_safety_cert, created_at, updated_at)
VALUES 
  ('a0000001-0000-0000-0000-000000000001', 'Chèo SUP & Kayak', 'Stand-Up Paddleboarding & Kayak', 'cheo-sup-kayak', NULL, 'surfing', true, true, NOW(), NOW()),
  ('a0000001-0000-0000-0000-000000000002', 'Lặn ngắm san hô & Đi bộ dưới biển', 'Scuba Diving & Seawalker', 'lan-ngam-san-ho', NULL, 'scuba_diving', true, true, NOW(), NOW()),
  ('a0000001-0000-0000-0000-000000000003', 'Cano lướt sóng & Dù bay biển', 'Speedboat & Parasailing', 'cano-du-bay', NULL, 'speed', true, true, NOW(), NOW()),
  ('a0000001-0000-0000-0000-000000000004', 'Mô tô nước (Jetski)', 'Jetski Rental', 'mo-to-nuoc-jetski', NULL, 'waves', true, true, NOW(), NOW()),
  ('a0000001-0000-0000-0000-000000000005', 'Trượt phao chuối cảm giác mạnh', 'Banana Boat Ride', 'truot-phao-chuoi', NULL, 'sports_kabaddi', true, true, NOW(), NOW()),
  ('a0000001-0000-0000-0000-000000000006', 'Du thuyền ngắm hoàng hôn vịnh', 'Luxury Sunset Yacht Tour', 'du-thuyen-ngam-hoang-hon', NULL, 'sailing', true, true, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- 2. Seed the 6 benchmark category safety rules linked to categories
INSERT INTO category_safety_rules (
    id, category_id, category_slug, category_name,
    caution_wave_height_m, max_wave_height_m,
    caution_wind_speed_kmh, max_wind_speed_kmh,
    max_wind_gust_kmh, max_ocean_current_ms, min_visibility_m,
    fatal_thunderstorm_codes, created_at, updated_at
)
SELECT 
    gen_random_uuid(), c.id, c.slug, c.name,
    v.caution_wave, v.max_wave,
    v.caution_wind, v.max_wind,
    v.max_gust, v.max_current, v.min_vis,
    '95,96,99', NOW(), NOW()
FROM (VALUES
    ('cheo-sup-kayak', 0.50, 0.80, 12.00, 20.00, 28.00, 0.30, 2000.00),
    ('lan-ngam-san-ho', 0.80, 1.20, 15.00, 25.00, 35.00, 0.50, 3000.00),
    ('cano-du-bay', 0.70, 1.00, 20.00, 28.00, 37.00, 0.80, 1800.00),
    ('mo-to-nuoc-jetski', 0.60, 1.20, 20.00, 30.00, 40.00, 0.60, 1500.00),
    ('truot-phao-chuoi', 0.50, 1.00, 18.00, 25.00, 35.00, 0.50, 2000.00),
    ('du-thuyen-ngam-hoang-hon', 0.80, 1.50, 25.00, 38.00, 48.00, 0.70, 2000.00)
) AS v(slug, caution_wave, max_wave, caution_wind, max_wind, max_gust, max_current, min_vis)
JOIN categories c ON c.slug = v.slug
ON CONFLICT (category_slug) DO NOTHING;

-- 3. Upgrade safety_rule_evaluations table
ALTER TABLE safety_rule_evaluations
    ADD COLUMN IF NOT EXISTS status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS alert_level VARCHAR(20),
    ADD COLUMN IF NOT EXISTS peak_wave_height_m NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS peak_wind_speed_kmh NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS peak_wind_gust_kmh NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS peak_ocean_current_ms NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS min_visibility_m NUMERIC(7,2),
    ADD COLUMN IF NOT EXISTS severe_weather_code INTEGER;

-- 4. Alter weather_caches raw_payload column to TEXT
ALTER TABLE weather_caches ALTER COLUMN raw_payload TYPE TEXT;
