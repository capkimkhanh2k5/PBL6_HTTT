package com.danasea.backend.modules.service.infrastructure.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;
import com.danasea.backend.modules.service.application.ports.FileStoragePort;
import com.danasea.backend.modules.service.infrastructure.storage.CloudinaryStorageAdapter;

@Configuration
public class ServiceInfrastructureConfig {

    @Bean
    public Cloudinary cloudinary(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        return new Cloudinary(config);
    }

    @Bean
    public FileStoragePort fileStoragePort(Cloudinary cloudinary) {
        return new CloudinaryStorageAdapter(cloudinary);
    }
}
