package com.danasea.backend.modules.vendor.application.ports;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentStoragePort {

    String uploadDocument(MultipartFile file, String folder);
}
