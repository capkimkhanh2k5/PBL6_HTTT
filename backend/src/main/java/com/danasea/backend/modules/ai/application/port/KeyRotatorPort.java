package com.danasea.backend.modules.ai.application.port;

public interface KeyRotatorPort {
    /**
     * Gets an active Groq API key.
     * @return an active API key string
     * @throws RuntimeException if no active keys are available
     */
    String getActiveKey();

    /**
     * Marks a key as COOLDOWN for a certain duration (e.g. after a 429 error).
     * @param key the API key to mark
     */
    void markKeyCooldown(String key);

    /**
     * Marks a key as DISABLED (e.g. after a 401 error).
     * @param key the API key to mark
     */
    void markKeyDisabled(String key);
}
