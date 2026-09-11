package com.danasea.backend.modules.service.application.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchServicesCriteria {
    private UUID categoryId;
    private String keyword;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal lat;
    private BigDecimal lng;
    private Double radiusKm;
    private int page;
    private int size;
}
