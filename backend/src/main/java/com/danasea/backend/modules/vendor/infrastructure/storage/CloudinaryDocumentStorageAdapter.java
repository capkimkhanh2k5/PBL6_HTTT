package com.danasea.backend.modules.vendor.infrastructure.storage;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.danasea.backend.modules.vendor.application.port.DocumentStoragePort;

@Component
public class CloudinaryDocumentStorageAdapter implements DocumentStoragePort {

    @Override
    public String uploadDocument(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
        String originalFilename = file.getOriginalFilename();
        String safeFilename = (originalFilename != null && !originalFilename.isBlank())
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "document";

        String targetFolder = (folder != null && !folder.isBlank()) ? folder : "vendor_documents";
        return "https://res.cloudinary.com/danasea/raw/upload/" + targetFolder + "/" + UUID.randomUUID() + "_" + safeFilename;
    }
}
