package com.danasea.backend.modules.ai.application.port;

import java.math.BigDecimal;
import java.util.List;
import com.danasea.backend.shared.i18n.SupportedLanguage;

public interface ServiceSearchPort {
    List<ServiceSearchResultDto> exactAndFilterSearch(String query, String category, BigDecimal minPrice, BigDecimal maxPrice);

    default List<ServiceSearchResultDto> exactAndFilterSearch(
            String query, String category, BigDecimal minPrice, BigDecimal maxPrice,
            SupportedLanguage language) {
        return exactAndFilterSearch(query, category, minPrice, maxPrice);
    }
}
