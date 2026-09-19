package com.danasea.backend.modules.ai.application.port;

import java.math.BigDecimal;
import java.util.List;

public interface ServiceSearchPort {
    List<ServiceSearchResultDto> exactAndFilterSearch(String query, String category, BigDecimal minPrice, BigDecimal maxPrice);
}
