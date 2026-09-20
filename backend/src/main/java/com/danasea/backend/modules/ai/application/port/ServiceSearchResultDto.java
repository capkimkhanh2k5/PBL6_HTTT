package com.danasea.backend.modules.ai.application.port;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceSearchResultDto {
    private UUID serviceId;
    private String name;
    private BigDecimal price;
    private BigDecimal avgRating;
    private String matchType; // e.g., "exact", "filter", "semantic"
}
