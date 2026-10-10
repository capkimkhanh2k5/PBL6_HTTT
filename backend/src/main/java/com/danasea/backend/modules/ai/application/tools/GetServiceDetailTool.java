package com.danasea.backend.modules.ai.application.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi;
import com.danasea.backend.modules.ai.application.dtos.TravelRequest;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.danasea.backend.modules.ai.domain.services.SanitizationService;
import com.danasea.backend.modules.service.application.dtos.ServiceDetailResult;
import com.danasea.backend.modules.service.application.usecases.GetPublicServiceDetailUseCase;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GetServiceDetailTool implements ToolExecutor {

    private final GetPublicServiceDetailUseCase getPublicServiceDetailUseCase;
    private final SanitizationService sanitizationService;
    private final ObjectMapper objectMapper;
    private AiCatalogReadApi catalog;

    @Autowired
    public void setCatalog(AiCatalogReadApi catalog) { this.catalog = catalog; }

    @Override
    public String getName() {
        return "get_service_detail";
    }

    @Override
    public String execute(String argumentsJson) { return execute(argumentsJson, null); }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext executionContext) {
        try {
            UUID serviceId = parseServiceId(argumentsJson);
            if (catalog != null) {
                JsonNode args = objectMapper.readTree(argumentsJson);
                ObjectNode supplied = args.get("criteria") instanceof ObjectNode criteria ? criteria : objectMapper.createObjectNode();
                var context = objectMapper.treeToValue(supplied, TravelRequest.class).context();
                SupportedLanguage language = executionContext == null || executionContext.language() == null
                        ? SupportedLanguage.VI : executionContext.language();
                var service = catalog.find(serviceId, new AiCatalogReadApi.Query(null, null, context.from(), context.to(),
                        context.partySize(), null, null, null, 1, language));
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("serviceId", service.id()); result.put("name", sanitize(service.name()));
                result.put("description", sanitize(service.description())); result.put("address", sanitize(service.address()));
                result.put("categoryId", service.categoryId()); result.put("categoryName", sanitize(service.categoryName()));
                result.put("latitude", service.latitude()); result.put("longitude", service.longitude());
                result.put("averageRating", service.averageRating()); result.put("reviewCount", service.reviewCount());
                result.put("options", service.options()); result.put("priceBasis", "ACTIVE_OPTION_PARTY_TOTAL");
                result.put("partySize", context.partySize()); result.put("from", context.from()); result.put("to", context.to());
                result.put("defaultedFields", context.criteria().defaultedFields());
                result.put("price", service.options().stream().map(AiCatalogReadApi.Option::partyTotal).min(java.math.BigDecimal::compareTo).orElse(null));
                result.put("availableSlots", service.options().stream().flatMap(option -> option.slots().stream()).toList());
                result.put("retrievedAt", java.time.Instant.now()); result.put("inventoryReserved", false);
                return objectMapper.writeValueAsString(result);
            }
            ServiceDetailResult detail = getPublicServiceDetailUseCase.readOnlySnapshot(serviceId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("serviceId", detail.getId());
            result.put("name", sanitize(detail.getName()));
            result.put("description", sanitize(detail.getDescription()));
            result.put("price", detail.getPrice());
            result.put("promotionalPrice", detail.getPromotionalPrice());
            result.put("address", sanitize(detail.getAddress()));
            result.put("latitude", detail.getLatitude());
            result.put("longitude", detail.getLongitude());
            result.put("averageRating", detail.getAverageRating());
            result.put("reviewCount", detail.getReviewCount());
            result.put("categoryId", detail.getCategoryId());
            result.put("categoryName", sanitize(detail.getCategoryName()));
            result.put("imageUrls", sanitize(detail.getImageUrls()));
            result.put("availableSlots", sanitize(detail.getAvailableSlots()));
            result.put("options", detail.getOptions() == null ? List.of() : detail.getOptions().stream().map(option -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("id", option.id()); value.put("name", sanitize(option.name()));
                value.put("pricingUnit", option.pricingUnit()); value.put("unitPrice", option.price());
                value.put("maxPaxPerPackage", option.maxPaxPerPackage()); value.put("benefits", sanitize(option.benefits()));
                return value;
            }).toList());
            return objectMapper.writeValueAsString(result);
        } catch (IllegalArgumentException exception) {
            return error("INVALID_ARGUMENTS", exception.getMessage());
        } catch (ServiceNotFoundException exception) {
            return error("SERVICE_NOT_FOUND", "The requested published service could not be found.");
        } catch (Exception exception) {
            return error("TOOL_EXECUTION_FAILED", "Service details are temporarily unavailable.");
        }
    }

    private UUID parseServiceId(String argumentsJson) throws Exception {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            throw new IllegalArgumentException("serviceId is required");
        }

        JsonNode arguments;
        try {
            arguments = objectMapper.readTree(argumentsJson);
        } catch (Exception exception) {
            throw new IllegalArgumentException("arguments must be valid JSON");
        }
        JsonNode serviceIdNode = firstPresent(arguments, "serviceId", "service_id", "id");
        if (serviceIdNode == null || serviceIdNode.isNull() || serviceIdNode.asText().isBlank()) {
            throw new IllegalArgumentException("serviceId is required");
        }

        try {
            return UUID.fromString(serviceIdNode.asText());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("serviceId must be a valid UUID");
        }
    }

    private JsonNode firstPresent(JsonNode arguments, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (arguments.has(fieldName)) {
                return arguments.get(fieldName);
            }
        }
        return null;
    }

    private String sanitize(String value) {
        return sanitizationService.sanitize(value);
    }

    private List<String> sanitize(List<String> values) {
        return values == null ? List.of() : values.stream().map(this::sanitize).toList();
    }

    private String error(String code, String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", code, "message", message));
        } catch (Exception ignored) {
            return "{\"error\":\"" + code + "\"}";
        }
    }
}
