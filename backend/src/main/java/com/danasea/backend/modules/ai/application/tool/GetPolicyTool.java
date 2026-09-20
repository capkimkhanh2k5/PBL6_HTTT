package com.danasea.backend.modules.ai.application.tool;

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

    @Autowired
    public GetPolicyTool(@Autowired(required = false) JpaSystemConfigRepository systemConfigRepository,
                         ObjectMapper objectMapper) {
        this.systemConfigRepository = systemConfigRepository;
        this.objectMapper = objectMapper;
    }

    public GetPolicyTool() {
        this(null, new ObjectMapper());
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
            case "CANCELLATION" -> "Hoàn tiền 100% khi hủy trước 24h. Hoàn 50% trước 12h. Không hoàn tiền trong vòng 12h trước giờ khởi hành.";
            case "REFUND" -> "Chính sách hoàn tiền: Hoàn 100% tiền cọc nếu huỷ đúng hạn hoặc thời tiết xấu không thể tổ chức tour.";
            case "WEATHER_CANCELLATION" -> "Chính sách hủy do thời tiết: Trường hợp điều kiện hàng hải nguy hiểm hoặc thiên tai, tour sẽ được hoàn tiền 100% hoặc đổi ngày miễn phí.";
            case "SAFETY" -> "Chính sách an toàn: Khách hàng bắt buộc mặc áo phao và tuân theo chỉ dẫn an toàn hàng hải của ban quản lý và hướng dẫn viên.";
            default -> "Chính sách chung DanaSea: Cam kết bảo vệ quyền lợi khách hàng, an toàn hàng hải và minh bạch dịch vụ du lịch biển Đà Nẵng.";
        };
    }
}

