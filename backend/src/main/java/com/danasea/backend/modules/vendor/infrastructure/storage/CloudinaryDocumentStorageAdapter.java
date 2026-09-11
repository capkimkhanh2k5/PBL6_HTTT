package com.danasea.backend.modules.vendor.infrastructure.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.danasea.backend.modules.vendor.application.port.DocumentStoragePort;

@Component
public class CloudinaryDocumentStorageAdapter implements DocumentStoragePort {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryDocumentStorageAdapter.class);

    private final Cloudinary cloudinary;

    public CloudinaryDocumentStorageAdapter(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadDocument(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        try {
            Map<String, Object> params = new HashMap<>();
            String targetFolder = (folder != null && !folder.isBlank()) ? folder : "vendor_documents";
            params.put("folder", targetFolder);
            params.put("resource_type", "auto");

            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && !originalFilename.isBlank()) {
                params.put("use_filename", true);
                params.put("unique_filename", true);
            }

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null) {
                secureUrl = uploadResult.get("url");
            }

            if (secureUrl == null) {
                throw new RuntimeException("Cloudinary upload did not return a valid URL.");
            }

            return secureUrl.toString();
        } catch (IOException e) {
            log.error("Failed to read file for Cloudinary upload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read file for Cloudinary upload: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to upload file to Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }
}
