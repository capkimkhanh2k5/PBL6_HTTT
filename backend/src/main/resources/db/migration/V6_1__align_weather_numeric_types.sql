-- Align persisted weather metrics with the Java Double mappings used by Hibernate.
ALTER TABLE category_safety_rules
    ALTER COLUMN caution_wave_height_m TYPE DOUBLE PRECISION,
    ALTER COLUMN max_wave_height_m TYPE DOUBLE PRECISION,
    ALTER COLUMN caution_wind_speed_kmh TYPE DOUBLE PRECISION,
    ALTER COLUMN max_wind_speed_kmh TYPE DOUBLE PRECISION,
    ALTER COLUMN max_wind_gust_kmh TYPE DOUBLE PRECISION,
    ALTER COLUMN max_ocean_current_ms TYPE DOUBLE PRECISION,
    ALTER COLUMN min_visibility_m TYPE DOUBLE PRECISION;

ALTER TABLE safety_rule_evaluations
    ALTER COLUMN peak_wave_height_m TYPE DOUBLE PRECISION,
    ALTER COLUMN peak_wind_speed_kmh TYPE DOUBLE PRECISION,
    ALTER COLUMN peak_wind_gust_kmh TYPE DOUBLE PRECISION,
    ALTER COLUMN peak_ocean_current_ms TYPE DOUBLE PRECISION,
    ALTER COLUMN min_visibility_m TYPE DOUBLE PRECISION;
