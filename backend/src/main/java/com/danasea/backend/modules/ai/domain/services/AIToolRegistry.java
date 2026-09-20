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
                        "query", Map.of("type", List.of("string", "null"), "description", "Search keywords"),
                        "min_price", Map.of("type", List.of("number", "null"), "description", "Minimum price"),
                        "max_price", Map.of("type", List.of("number", "null"), "description", "Maximum price"),
                        "category", Map.of("type", List.of("string", "null"), "description", "Category ID or name")
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
                "name", "get_policy",
                "description", "Get the exact policy text from system configs for cancellation, refunds, weather cancellation, or safety",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "policy_type", Map.of(
                            "type", "string",
                            "enum", List.of("CANCELLATION", "REFUND", "WEATHER_CANCELLATION", "SAFETY", "GENERAL"),
                            "description", "Type of policy to retrieve: CANCELLATION, REFUND, WEATHER_CANCELLATION, SAFETY, or GENERAL"
                        )
                    ),
                    "required", List.of("policy_type")
                )
            ),
            Map.of(
                "name", "get_weather_forecast",
                "description", "Get weather forecast and sea conditions for a specific location and date in Da Nang",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "location", Map.of("type", "string", "description", "Location in Da Nang (e.g., Sơn Trà, Mỹ Khê, Man Thái)"),
                        "date", Map.of("type", "string", "description", "Date in YYYY-MM-DD format")
                    ),
                    "required", List.of("location", "date")
                )
            ),
            Map.of(
                "name", "get_safety_alert",
                "description", "Get maritime safety status, wave alerts, and danger warnings for sea activities in Da Nang",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "location", Map.of("type", "string", "description", "Location in Da Nang (e.g., Bán đảo Sơn Trà, Bãi biển Mỹ Khê)")
                    ),
                    "required", List.of("location")
                )
            ),
            Map.of(
                "name", "request_booking_confirmation",
                "description", "Request confirmation from the user to proceed with booking a service",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "service_id", Map.of("type", "string", "description", "The ID of the service to book"),
                        "date", Map.of("type", "string", "description", "Booking date"),
                        "participants", Map.of("type", "integer", "description", "Number of participants")
                    ),
                    "required", List.of("service_id", "date", "participants")
                )
            )
        );
    }
}
