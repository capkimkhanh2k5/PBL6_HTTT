package com.danasea.backend.modules.ai.domain.services;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AIToolRegistry {

    public List<Map<String, Object>> getToolDefinitions() {
        return List.of(
            Map.of(
                "name", "search_services",
                "description", "Search for travel and marine services based on user criteria",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "query", Map.of("type", "string", "description", "Search keywords"),
                        "min_price", Map.of("type", "number", "description", "Minimum price"),
                        "max_price", Map.of("type", "number", "description", "Maximum price"),
                        "category", Map.of("type", "string", "description", "Category ID or name")
                    )
                )
            ),
            Map.of(
                "name", "get_service_detail",
                "description", "Get detailed information about a specific service, including real-time price and availability",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "service_id", Map.of("type", "string", "description", "The ID of the service")
                    ),
                    "required", List.of("service_id")
                )
            ),
            Map.of(
                "name", "get_cancellation_policy",
                "description", "Get the exact cancellation policy for a specific service or general platform policy",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "service_id", Map.of("type", "string", "description", "Optional service ID")
                    )
                )
            )
        );
    }
}
