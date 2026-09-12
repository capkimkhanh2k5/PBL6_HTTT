package com.danasea.backend.modules.service.infrastructure.configs;

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
    public FileStoragePort fileStoragePort(Cloudinary cloudinary) {
        return new CloudinaryStorageAdapter(cloudinary);
    }
}
