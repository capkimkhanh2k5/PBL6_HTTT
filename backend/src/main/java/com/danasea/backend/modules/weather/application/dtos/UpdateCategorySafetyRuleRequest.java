package com.danasea.backend.modules.weather.application.dtos;

import java.util.Collection;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategorySafetyRuleRequest {

    @PositiveOrZero(message = "Ngưỡng sóng vàng không được âm")
    private Double cautionWaveHeightM;

    @PositiveOrZero(message = "Ngưỡng sóng đỏ không được âm")
    private Double maxWaveHeightM;

    @PositiveOrZero(message = "Ngưỡng gió duy trì vàng không được âm")
    private Double cautionWindSpeedKmh;

    @PositiveOrZero(message = "Ngưỡng gió duy trì đỏ không được âm")
    private Double maxWindSpeedKmh;

    @PositiveOrZero(message = "Ngưỡng gió giật đỏ không được âm")
    private Double maxWindGustKmh;

    @PositiveOrZero(message = "Ngưỡng dòng hải lưu đỏ không được âm")
    private Double maxOceanCurrentMs;

    @PositiveOrZero(message = "Tầm nhìn tối thiểu không được âm")
    private Double minVisibilityM;

    private String fatalThunderstormCodes;

    @JsonSetter("fatalThunderstormCodes")
    public void setFatalThunderstormCodes(Object value) {
        if (value == null) {
            this.fatalThunderstormCodes = null;
        } else if (value instanceof Collection<?> col) {
            this.fatalThunderstormCodes = col.stream().map(Object::toString).collect(Collectors.joining(","));
        } else {
            this.fatalThunderstormCodes = value.toString();
        }
    }
}
