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
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.i18n.SupportedLanguage;

@Slf4j
@Component
public class GetPolicyTool implements ToolExecutor {

    private final JpaSystemConfigRepository systemConfigRepository;
    private final ObjectMapper objectMapper;
    private final RefundPolicyEngine refundPolicyEngine;
    private LocalizedMessageService messages = LocalizedMessageService.standalone();

    @Autowired
    void setLocalizedMessageService(LocalizedMessageService messages) {
        this.messages = messages;
    }

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
        return executeInternal(argumentsJson, null);
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext context) {
        return executeInternal(argumentsJson,
                context == null || context.language() == null ? SupportedLanguage.VI : context.language());
    }

    private String executeInternal(String argumentsJson, SupportedLanguage requestedLanguage) {
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

        String policyText = fetchPolicyText(policyType, requestedLanguage);

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

    private String fetchPolicyText(String policyType, SupportedLanguage language) {
        if (systemConfigRepository != null) {
            try {
                String canonicalKey = "POLICY_" + policyType.toUpperCase();
                Optional<SystemConfigJpaEntity> entity;
                if (language == null) {
                    entity = systemConfigRepository.findByKey(canonicalKey);
                } else {
                    entity = systemConfigRepository.findByKey(canonicalKey + "." + language.code());
                    if (entity.isEmpty() && language == SupportedLanguage.EN) {
                        entity = systemConfigRepository.findByKey(canonicalKey + ".vi");
                    }
                    if (entity.isEmpty()) {
                        entity = systemConfigRepository.findByKey(canonicalKey);
                    }
                }
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

        if (language == null) {
            return getLegacyDefaultPolicyText(policyType);
        }
        return getDefaultPolicyText(policyType, language);
    }

    private String getLegacyDefaultPolicyText(String policyType) {
        return switch (policyType.toUpperCase()) {
            case "CANCELLATION" -> refundPolicyEngine.getCancellationPolicySummary();
            case "REFUND" -> refundPolicyEngine.getRefundPolicySummary("REFUND");
            case "WEATHER_CANCELLATION" -> "Chính sách hủy do thời tiết: Trường hợp điều kiện hàng hải nguy hiểm hoặc thiên tai, tour sẽ được hoàn tiền 100% hoặc đổi ngày miễn phí.";
            case "SAFETY" -> "Chính sách an toàn: Khách hàng bắt buộc mặc áo phao và tuân theo chỉ dẫn an toàn hàng hải của ban quản lý và hướng dẫn viên.";
            default -> "Chính sách chung DanaSea: Cam kết bảo vệ quyền lợi khách hàng, an toàn hàng hải và minh bạch dịch vụ du lịch biển Đà Nẵng.";
        };
    }

    private String getDefaultPolicyText(String policyType, SupportedLanguage language) {
        String suffix = switch (policyType.toUpperCase()) {
            case "CANCELLATION" -> "cancellation";
            case "REFUND" -> "refund";
            case "WEATHER_CANCELLATION" -> "weather_cancellation";
            case "SAFETY" -> "safety";
            default -> "general";
        };
        return messages.get("policy." + suffix, language);
    }
}
