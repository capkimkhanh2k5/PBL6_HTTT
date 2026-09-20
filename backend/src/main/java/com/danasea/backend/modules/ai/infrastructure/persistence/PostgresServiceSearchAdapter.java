package com.danasea.backend.modules.ai.infrastructure.persistence;

import com.danasea.backend.modules.ai.application.port.ServiceSearchPort;
import com.danasea.backend.modules.ai.application.port.ServiceSearchResultDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostgresServiceSearchAdapter implements ServiceSearchPort {

    private static final int DEFAULT_SEARCH_LIMIT = 5;
    private static final String MATCH_TYPE_EXACT = "exact";

    private final EntityManager entityManager;

    @Override
    public List<ServiceSearchResultDto> exactAndFilterSearch(String keyword, String category, BigDecimal minPrice,
            BigDecimal maxPrice) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, name, price, avg_rating ");
        sql.append("FROM services ");
        sql.append("WHERE status = 'PUBLISHED' ");

        List<Object> params = new ArrayList<>();
        int paramIndex = 1;

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND search_vector @@ plainto_tsquery('simple', ?) ");
            params.add(keyword.trim());
            paramIndex++;
        }

        if (minPrice != null) {
            sql.append("AND price >= ? ");
            params.add(minPrice);
            paramIndex++;
        }

        if (maxPrice != null) {
            sql.append("AND price <= ? ");
            params.add(maxPrice);
            paramIndex++;
        }

        if (category != null && !category.trim().isEmpty()) {
            sql.append("AND category_id IN (SELECT id FROM categories WHERE name ILIKE ?) ");
            params.add("%" + category.trim() + "%");
            paramIndex++;
        }

        sql.append("ORDER BY avg_rating DESC NULLS LAST LIMIT ").append(DEFAULT_SEARCH_LIMIT);

        Query query = entityManager.createNativeQuery(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }

        List<Object[]> results = query.getResultList();
        List<ServiceSearchResultDto> dtos = new ArrayList<>();

        for (Object[] row : results) {
            dtos.add(ServiceSearchResultDto.builder()
                    .serviceId((UUID) row[0])
                    .name((String) row[1])
                    .price((BigDecimal) row[2])
                    .avgRating((BigDecimal) row[3])
                    .matchType(MATCH_TYPE_EXACT)
                    .build());
        }

        return dtos;
    }
}
