package com.danasea.backend.modules.ai.application.tool;

import com.danasea.backend.modules.ai.application.port.ServiceSearchPort;
import com.danasea.backend.modules.ai.application.port.ServiceSearchResultDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SearchServiceTool implements ToolExecutor {

    private final ServiceSearchPort serviceSearchPort;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "search_services";
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            
            String query = args.has("query") && !args.get("query").isNull() ? args.get("query").asText() : null;
            String category = args.has("category") && !args.get("category").isNull() ? args.get("category").asText() : null;
            
            BigDecimal minPrice = null;
            if (args.has("min_price") && !args.get("min_price").isNull()) {
                minPrice = new BigDecimal(args.get("min_price").asText());
            }
            
            BigDecimal maxPrice = null;
            if (args.has("max_price") && !args.get("max_price").isNull()) {
                maxPrice = new BigDecimal(args.get("max_price").asText());
            }

            List<ServiceSearchResultDto> results = serviceSearchPort.exactAndFilterSearch(query, category, minPrice, maxPrice);

            return objectMapper.writeValueAsString(Map.of("results", results));
        } catch (Exception e) {
            return "{\"error\": \"Failed to execute search_services: " + e.getMessage() + "\"}";
        }
    }
}
