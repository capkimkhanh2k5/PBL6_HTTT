package com.danasea.backend.modules.service.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ServiceOption extends BaseDomainModel {
    private UUID serviceId;
    private String name;
    private OptionType optionType;
    private PricingUnit pricingUnit;
    private BigDecimal price;
    private Integer maxPaxPerPackage;
    private String benefits;
    private OptionStatus status;

    public boolean isActive() {
        return OptionStatus.ACTIVE.equals(this.status);
    }

    public boolean isPrivate() {
        return OptionType.PRIVATE.equals(this.optionType);
    }

    public boolean isShared() {
        return OptionType.SHARED.equals(this.optionType);
    }
}
