package com.danasea.backend.modules.ai.application.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.i18n.SupportedLanguage;

@Component
public class RequestBookingConfirmationTool implements ToolExecutor {

    private static final String STATUS_CONFIRMATION_PENDING = "CONFIRMATION_PENDING";
    private static final String DEFAULT_CONFIRMATION_ID = "CONF-9999";
    private final ObjectMapper objectMapper;
    private LocalizedMessageService messages = LocalizedMessageService.standalone();

    @Autowired
    void setLocalizedMessageService(LocalizedMessageService messages) {
        this.messages = messages;
    }

    @Autowired
    public RequestBookingConfirmationTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RequestBookingConfirmationTool() {
        this(new ObjectMapper());
    }

    @Override
    public String getName() {
        return "request_booking_confirmation";
    }

    @Override
    public String execute(String argumentsJson) {
        return execute(argumentsJson, new ToolExecutionContext(SupportedLanguage.VI, null));
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext context) {
        String confirmationMessage = messages.get("ai.booking.confirmation", context.language());
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "status", STATUS_CONFIRMATION_PENDING,
                    "confirmation_id", DEFAULT_CONFIRMATION_ID,
                    "message", confirmationMessage
            ));
        } catch (Exception e) {
            return String.format("{\"status\": \"%s\", \"confirmation_id\": \"%s\", \"message\": \"%s\"}",
                    STATUS_CONFIRMATION_PENDING, DEFAULT_CONFIRMATION_ID, confirmationMessage);
        }
    }
}
