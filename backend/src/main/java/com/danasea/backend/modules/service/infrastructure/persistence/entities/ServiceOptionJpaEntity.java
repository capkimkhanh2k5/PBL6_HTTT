package com.danasea.backend.modules.service.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.OptionStatus;
import com.danasea.backend.modules.service.domain.models.OptionType;
import com.danasea.backend.modules.service.domain.models.PricingUnit;
import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceOptionJpaEntity extends BaseJpaEntity {

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false)
    private OptionType optionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_unit", nullable = false)
    private PricingUnit pricingUnit;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "max_pax_per_package")
    private Integer maxPaxPerPackage;

    @Column(name = "benefits", columnDefinition = "TEXT")
    private String benefits;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OptionStatus status;
}
