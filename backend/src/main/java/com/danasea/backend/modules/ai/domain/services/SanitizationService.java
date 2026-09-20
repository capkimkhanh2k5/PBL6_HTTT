package com.danasea.backend.modules.ai.domain.services;

import org.springframework.stereotype.Service;

@Service
public class SanitizationService {
    
    /**
     * Sanitizes input text to prevent prompt injection or malicious instructions.
     */
    public String sanitize(String input) {
        if (input == null) return null;
        // Basic sanitization: remove common prompt injection keywords
        String sanitized = input.replaceAll("(?i)(ignore previous instructions|system prompt|you are a|forget everything|bypass)", "[REDACTED]");
        // Escape special characters that might confuse LLMs if injected in JSON or quotes
        sanitized = sanitized.replace("\"", "\\\"");
        return sanitized;
    }
}
