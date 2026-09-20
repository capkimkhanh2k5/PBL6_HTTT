package com.danasea.backend.modules.ai.application.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RequestBookingConfirmationTool implements ToolExecutor {

    private static final String STATUS_CONFIRMATION_PENDING = "CONFIRMATION_PENDING";
    private static final String DEFAULT_CONFIRMATION_ID = "CONF-9999";
    private static final String DEFAULT_MESSAGE = "Please confirm the booking details.";

    private final ObjectMapper objectMapper;

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
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "status", STATUS_CONFIRMATION_PENDING,
                    "confirmation_id", DEFAULT_CONFIRMATION_ID,
                    "message", DEFAULT_MESSAGE
            ));
        } catch (Exception e) {
            return String.format("{\"status\": \"%s\", \"confirmation_id\": \"%s\", \"message\": \"%s\"}",
                    STATUS_CONFIRMATION_PENDING, DEFAULT_CONFIRMATION_ID, DEFAULT_MESSAGE);
        }
    }
}
