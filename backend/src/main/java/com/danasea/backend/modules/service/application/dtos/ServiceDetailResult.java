package com.danasea.backend.modules.service.application.dtos;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ServiceDetailResult {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal promotionalPrice;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal averageRating;
    private int reviewCount;
    private int viewCount;
    private UUID categoryId;
    private String categoryName;
    private List<String> imageUrls;
    private List<String> availableSlots;
}
