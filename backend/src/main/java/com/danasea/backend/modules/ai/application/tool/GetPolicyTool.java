package com.danasea.backend.modules.ai.application.tool;

import com.danasea.backend.modules.order.domain.services.RefundPolicyEngine;
import com.danasea.backend.modules.systemconfig.infrastructure.persistence.entities.SystemConfigJpaEntity;
import com.danasea.backend.modules.systemconfig.infrastructure.persistence.repositories.JpaSystemConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class GetPolicyTool implements ToolExecutor {

    private final JpaSystemConfigRepository systemConfigRepository;
    private final ObjectMapper objectMapper;
    private final RefundPolicyEngine refundPolicyEngine;

    @Autowired
    public GetPolicyTool(
            @Autowired(required = false) JpaSystemConfigRepository systemConfigRepository,
            ObjectMapper objectMapper,
            @Autowired(required = false) RefundPolicyEngine refundPolicyEngine) {
        this.systemConfigRepository = systemConfigRepository;
        this.objectMapper = objectMapper;
        this.refundPolicyEngine = refundPolicyEngine != null ? refundPolicyEngine : new RefundPolicyEngine();
    }

    public GetPolicyTool(JpaSystemConfigRepository systemConfigRepository, ObjectMapper objectMapper) {
        this(systemConfigRepository, objectMapper, new RefundPolicyEngine());
    }

    public GetPolicyTool() {
        this(null, new ObjectMapper(), new RefundPolicyEngine());
    }

    @Override
    public String getName() {
        return "get_policy";
    }

    @Override
    public String execute(String argumentsJson) {
        String policyType = "GENERAL";
        try {
            if (argumentsJson != null && !argumentsJson.isBlank()) {
                JsonNode root = objectMapper.readTree(argumentsJson);
                if (root.has("policy_type") && !root.get("policy_type").isNull()) {
                    policyType = root.get("policy_type").asText();
                } else if (root.has("policyType") && !root.get("policyType").isNull()) {
                    policyType = root.get("policyType").asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse arguments for get_policy: {}", argumentsJson, e);
        }

        String policyText = fetchPolicyText(policyType);

        Map<String, Object> response = new HashMap<>();
        response.put("policy_type", policyType);
        response.put("text", policyText);
        response.put("policy", policyText);

        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return String.format("{\"policy_type\":\"%s\",\"text\":\"%s\"}", policyType, policyText);
        }
    }

    private String fetchPolicyText(String policyType) {
        if (systemConfigRepository != null) {
            try {
                Optional<SystemConfigJpaEntity> entity = systemConfigRepository.findByKey("POLICY_" + policyType.toUpperCase());
                if (entity.isEmpty()) {
                    entity = systemConfigRepository.findByKey("policy_" + policyType.toLowerCase());
                }
                if (entity.isEmpty()) {
                    entity = systemConfigRepository.findByKey(policyType);
                }
                if (entity.isPresent() && entity.get().getValue() != null && !entity.get().getValue().isBlank()) {
                    return entity.get().getValue();
                }
            } catch (Exception e) {
                log.warn("Database lookup for policy {} failed, falling back to default", policyType, e);
            }
        }

        return getDefaultPolicyText(policyType);
    }

    private String getDefaultPolicyText(String policyType) {
        return switch (policyType.toUpperCase()) {
            case "CANCELLATION" -> refundPolicyEngine != null
                    ? refundPolicyEngine.getCancellationPolicySummary()
                    : "Refunds are 100% more than 48 hours before departure, 70% from 24 to 48 hours, 30% from 2 to 24 hours, and 0% within 2 hours. Dangerous weather and vendor fault receive a full refund.";
            case "REFUND" -> refundPolicyEngine != null
                    ? refundPolicyEngine.getRefundPolicySummary("REFUND")
                    : "Refund policy: eligible on-time cancellations and tours cancelled for dangerous weather receive the applicable refund.";
            case "WEATHER_CANCELLATION" -> "Weather cancellation policy: dangerous marine weather or natural disasters qualify for a full refund or free rescheduling.";
            case "SAFETY" -> "Safety policy: customers must wear life jackets and follow all instructions from staff and guides.";
            default -> "DanaSea policy: protect customers, enforce marine safety, and provide transparent services.";
        };
    }
}
