package com.danasea.backend.modules.ai.application.tool;

import com.danasea.backend.modules.ai.application.port.ServiceSearchPort;
import com.danasea.backend.modules.ai.application.port.ServiceSearchResultDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchServiceTool implements ToolExecutor {

    private static final int MAX_QUERY_LENGTH = 200;
    private static final int MAX_CATEGORY_LENGTH = 100;

    private final ServiceSearchPort serviceSearchPort;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "search_services";
    }

    @Override
    public String execute(String argumentsJson) {
        return execute(argumentsJson, null);
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext context) {
        try {
            JsonNode args = argumentsJson == null || argumentsJson.isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(argumentsJson);
            if (args == null || !args.isObject()) {
                return error("INVALID_ARGUMENTS", "arguments must be a JSON object");
            }

            String query = optionalText(args, "query");
            String category = optionalText(args, "category");
            if (query != null && query.length() > MAX_QUERY_LENGTH) {
                return error("INVALID_ARGUMENTS", "query must not exceed 200 characters");
            }
            if (category != null && category.length() > MAX_CATEGORY_LENGTH) {
                return error("INVALID_ARGUMENTS", "category must not exceed 100 characters");
            }

            BigDecimal minPrice = optionalDecimal(args, "min_price");
            BigDecimal maxPrice = optionalDecimal(args, "max_price");
            if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)) {
                return error("INVALID_ARGUMENTS", "prices must not be negative");
            }
            if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
                return error("INVALID_ARGUMENTS", "min_price must not exceed max_price");
            }

            List<ServiceSearchResultDto> results = context == null
                    ? serviceSearchPort.exactAndFilterSearch(query, category, minPrice, maxPrice)
                    : serviceSearchPort.exactAndFilterSearch(
                            query, category, minPrice, maxPrice, context.language());

            return objectMapper.writeValueAsString(Map.of("results", results == null ? List.of() : results));
        } catch (IllegalArgumentException exception) {
            return error("INVALID_ARGUMENTS", exception.getMessage());
        } catch (Exception exception) {
            log.warn("Service search tool execution failed", exception);
            return error("SEARCH_UNAVAILABLE", "Service search is temporarily unavailable");
        }
    }

    private String optionalText(JsonNode args, String field) {
        if (!args.has(field) || args.get(field).isNull()) {
            return null;
        }
        if (!args.get(field).isTextual()) {
            throw new IllegalArgumentException(field + " must be a string");
        }
        String value = args.get(field).asText().trim();
        return value.isEmpty() ? null : value;
    }

    private BigDecimal optionalDecimal(JsonNode args, String field) {
        if (!args.has(field) || args.get(field).isNull()) {
            return null;
        }
        if (!args.get(field).isNumber()) {
            throw new IllegalArgumentException(field + " must be a number");
        }
        return args.get(field).decimalValue();
    }

    private String error(String code, String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", code, "message", message));
        } catch (Exception exception) {
            return "{\"error\":\"" + code + "\"}";
        }
    }
}
