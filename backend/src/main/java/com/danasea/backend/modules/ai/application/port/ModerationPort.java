package com.danasea.backend.modules.ai.application.port;

public interface ModerationPort {
    /**
     * Checks if the content is safe and does not contain prompt injections.
     * 
     * @param content The text content to check.
     * @return true if the content is safe, false if prompt injection or unsafe content is detected.
     */
    boolean isSafe(String content);

    /**
     * Alias for isSafe(content) to check prompt injection safety.
     *
     * @param content The text content to check.
     * @return true if the content is safe, false if prompt injection is detected.
     */
    default boolean check(String content) {
        return isSafe(content);
    }
}
