package com.danasea.backend.modules.ai.application.tool;

import com.danasea.backend.modules.ai.domain.services.SanitizationService;
import com.danasea.backend.modules.service.application.dtos.ServiceDetailResult;
import com.danasea.backend.modules.service.application.usecases.GetPublicServiceDetailUseCase;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetServiceDetailTool implements ToolExecutor {

    private final GetPublicServiceDetailUseCase getPublicServiceDetailUseCase;
    private final SanitizationService sanitizationService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "get_service_detail";
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            UUID serviceId = parseServiceId(argumentsJson);
            ServiceDetailResult detail = getPublicServiceDetailUseCase.execute(serviceId, null, null);

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
