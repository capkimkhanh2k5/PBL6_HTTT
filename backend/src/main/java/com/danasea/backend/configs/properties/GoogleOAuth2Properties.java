package com.danasea.backend.configs.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth2.google")
public record GoogleOAuth2Properties(
        String clientId,
        String clientSecret
) {
}
