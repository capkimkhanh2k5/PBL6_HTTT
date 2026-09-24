package com.danasea.backend.configs;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.danasea.backend.configs.properties.CloudinaryProperties;
import com.cloudinary.Cloudinary;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", properties.cloudName());
        config.put("api_key", properties.apiKey());
        config.put("api_secret", properties.apiSecret());
        return new Cloudinary(config);
    }
}
