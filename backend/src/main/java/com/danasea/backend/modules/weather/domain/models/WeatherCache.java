package com.danasea.backend.modules.weather.domain.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WeatherCache extends BaseDomainModel {
    private String locationKey;
    private BigDecimal windSpeedKmh;
    private BigDecimal waveHeightM;
    private BigDecimal precipitationMm;
    private String rawPayload;
    private OffsetDateTime fetchedAt;
    private OffsetDateTime expiresAt;
}
