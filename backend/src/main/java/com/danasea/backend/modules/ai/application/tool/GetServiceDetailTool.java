package com.danasea.backend.modules.ai.application.tool;

import org.springframework.stereotype.Component;
import com.danasea.backend.modules.ai.domain.services.SanitizationService;

@Component
public class GetServiceDetailTool implements ToolExecutor {

    private final SanitizationService sanitizationService;

    public GetServiceDetailTool(SanitizationService sanitizationService) {
        this.sanitizationService = sanitizationService;
    }

    @Override
    public String getName() {
        return "get_service_detail";
    }

    @Override
    public String execute(String argumentsJson) {
        // TODO: call catalog port and get service details

        return "TODO";
    }
}
