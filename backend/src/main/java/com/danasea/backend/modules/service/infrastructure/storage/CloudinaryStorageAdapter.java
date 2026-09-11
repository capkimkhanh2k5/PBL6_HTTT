package com.danasea.backend.modules.service.infrastructure.storage;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudinary.Cloudinary;
import com.danasea.backend.modules.service.application.ports.FileStoragePort;
import com.danasea.backend.modules.service.domain.exceptions.FileStorageException;

public class CloudinaryStorageAdapter implements FileStoragePort {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryStorageAdapter.class);

    private final Cloudinary cloudinary;

    public CloudinaryStorageAdapter(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadFile(byte[] fileData, String originalFilename, String folder) {
        if (fileData == null || fileData.length == 0) {
            throw new FileStorageException("Cannot upload empty file.");
        }

        try {
            Map<String, Object> params = new HashMap<>();
            if (folder != null && !folder.isBlank()) {
                params.put("folder", folder);
            }
            params.put("resource_type", "auto");

            if (originalFilename != null && !originalFilename.isBlank()) {
                params.put("use_filename", true);
                params.put("unique_filename", true);
            }

            Map<?, ?> uploadResult = cloudinary.uploader().upload(fileData, params);
            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null) {
                secureUrl = uploadResult.get("url");
            }

            if (secureUrl == null) {
                throw new FileStorageException("Cloudinary upload did not return a valid URL.");
            }

            return secureUrl.toString();
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload file to Cloudinary: {}", e.getMessage(), e);
            throw new FileStorageException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            String publicId = extractPublicId(fileUrl);
            if (publicId == null || publicId.isBlank()) {
                log.warn("Could not extract publicId from fileUrl: {}", fileUrl);
                return;
            }

            Map<String, Object> params = new HashMap<>();
            params.put("invalidate", true);
            if (fileUrl.contains("/raw/")) {
                params.put("resource_type", "raw");
            }

            cloudinary.uploader().destroy(publicId, params);
        } catch (Exception e) {
            log.error("Failed to delete file from Cloudinary: {}", e.getMessage(), e);
            throw new FileStorageException("Failed to delete file from Cloudinary: " + e.getMessage(), e);
        }
    }

    public String extractPublicId(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
            return fileUrl;
        }

        int uploadIndex = fileUrl.indexOf("/upload/");
        if (uploadIndex == -1) {
            return fileUrl;
        }

        String pathAfterUpload = fileUrl.substring(uploadIndex + "/upload/".length());

        // Strip version or transformations followed by version (e.g. "v1234567890/" or "c_fill,w_300/v1234567890/")
        if (pathAfterUpload.matches("^v\\d+/.*")) {
            pathAfterUpload = pathAfterUpload.substring(pathAfterUpload.indexOf('/') + 1);
        } else {
            int vIndex = pathAfterUpload.indexOf("/v");
            if (vIndex != -1) {
                int slashAfterV = pathAfterUpload.indexOf('/', vIndex + 2);
                if (slashAfterV != -1 && pathAfterUpload.substring(vIndex + 2, slashAfterV).matches("\\d+")) {
                    pathAfterUpload = pathAfterUpload.substring(slashAfterV + 1);
                }
            }
        }

        int dotIndex = pathAfterUpload.lastIndexOf('.');
        if (dotIndex != -1) {
            pathAfterUpload = pathAfterUpload.substring(0, dotIndex);
        }

        return pathAfterUpload;
    }
}
