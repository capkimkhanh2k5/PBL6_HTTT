package com.danasea.backend.modules.ai.domain.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CustomerFeatureToolDefinitions {
    private CustomerFeatureToolDefinitions() {}

    public static List<Map<String, Object>> definitions() {
        Map<String, Object> criteria = Map.of("type", "object", "properties", Map.ofEntries(
                Map.entry("query", Map.of("type", "string")),
                Map.entry("partySize", Map.of("type", "integer", "minimum", 1, "maximum", 50)),
                Map.entry("totalBudget", Map.of("type", "number", "description", "Total VND budget for the entire party")),
                Map.entry("from", Map.of("type", "string", "format", "date")),
                Map.entry("to", Map.of("type", "string", "format", "date")),
                Map.entry("dayStart", Map.of("type", "string", "description", "HH:mm daily start")),
                Map.entry("dayEnd", Map.of("type", "string", "description", "HH:mm daily end")),
                Map.entry("latitude", Map.of("type", "number")),
                Map.entry("longitude", Map.of("type", "number")),
                Map.entry("radiusKm", Map.of("type", "number")),
                Map.entry("interests", Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 10)),
                Map.entry("limit", Map.of("type", "integer", "maximum", 20)),
                Map.entry("maxActivities", Map.of("type", "integer", "minimum", 1, "maximum", 5)),
                Map.entry("weatherSafeOnly", Map.of("type", "boolean"))));
        return List.of(
                definition("ai_smart_search", "Search published services using natural language and explicit party, budget and date constraints. Prices are current active-option party quotes.", Map.of("criteria", criteria), List.of("criteria")),
                definition("ai_recommend_services", "Recommend currently bookable services using provided preferences and optional Quyet relevance suggestions. This is not a proven personalized ranking model.", Map.of("criteria", criteria), List.of("criteria")),
                definition("ai_nearby_services", "Find currently bookable experiences near explicit coordinates; ask for location if missing.", Map.of("criteria", criteria), List.of("criteria")),
                definition("ai_compare_services", "Compare two to four published services using source data and current option/party prices.", Map.of("criteria", criteria, "serviceIds", ids(4)), List.of("criteria", "serviceIds")),
                definition("ai_plan_itinerary", "Create an owned itinerary proposal with current slots, budget and estimated transfers. It reserves no inventory and charges nothing.", Map.of("criteria", criteria), List.of("criteria")),
                definition("ai_replan_itinerary", "Replan an owned itinerary at an expected version after explicit user request. It does not change existing bookings.", Map.of("criteria", criteria, "itineraryId", uuid(), "expectedVersion", Map.of("type", "integer", "minimum", 0), "excludedServiceIds", ids(20), "excludedSlotIds", ids(50), "trigger", Map.of("type", "string")), List.of("itineraryId", "expectedVersion")),
                definition("ai_review_summary", "Read an extractive summary with real public review IDs, rating counts and tentative Quyet aspect/sentiment suggestions. No reviews means no claims.", Map.of("serviceId", uuid()), List.of("serviceId")),
                definition("ai_weather_slot", "Evaluate backend weather rules for a specific current service option and slot; unknown data never authorizes booking.", Map.of("criteria", criteria, "serviceId", uuid(), "optionId", uuid(), "slotId", uuid()), List.of("criteria", "serviceId", "optionId", "slotId")),
                definition("ai_customer_support", "Read the authenticated customer's own order and optional cancellation preview. Ask for an order ID if missing. Never cancel, refund or change payment state.", Map.of("orderId", uuid(), "message", Map.of("type", "string"), "includeCancellationPreview", Map.of("type", "boolean")), List.of("message")));
    }

    private static Map<String, Object> definition(String name, String description, Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object"); schema.put("properties", properties); schema.put("required", required);
        return Map.of("name", name, "description", description, "parameters", schema);
    }
    private static Map<String, Object> uuid() { return Map.of("type", "string", "format", "uuid"); }
    private static Map<String, Object> ids(int maximum) { return Map.of("type", "array", "items", uuid(), "maxItems", maximum); }
}
