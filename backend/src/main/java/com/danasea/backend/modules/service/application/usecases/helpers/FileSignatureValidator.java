package com.danasea.backend.modules.service.application.usecases.helpers;

import com.danasea.backend.modules.service.domain.exceptions.InvalidFileTypeException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class FileSignatureValidator {

    private FileSignatureValidator() {
    }

    public static byte[] readAndValidate(
            MultipartFile file,
            Set<String> allowedContentTypes,
            long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileTypeException("Uploaded file must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new InvalidFileTypeException("Uploaded file type is not allowed");
        }
        if (file.getSize() > maxBytes) {
            throw new InvalidFileTypeException("Uploaded file exceeds the maximum allowed size");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read uploaded file", exception);
        }

        if (!signatureMatches(contentType, bytes)) {
            throw new InvalidFileTypeException("Uploaded file content does not match its declared type");
        }
        return bytes;
    }

    private static boolean signatureMatches(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> startsWith(bytes, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "image/png" -> startsWith(bytes,
                    new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "image/gif" -> startsWith(bytes, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                    || startsWith(bytes, "GIF89a".getBytes(StandardCharsets.US_ASCII));
            case "image/webp" -> startsWith(bytes, "RIFF".getBytes(StandardCharsets.US_ASCII))
                    && bytes.length >= 12
                    && matchesAt(bytes, 8, "WEBP".getBytes(StandardCharsets.US_ASCII));
            case "application/pdf" -> startsWith(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            default -> false;
        };
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        return matchesAt(value, 0, prefix);
    }

    private static boolean matchesAt(byte[] value, int offset, byte[] expected) {
        if (value.length < offset + expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if (value[offset + index] != expected[index]) {
                return false;
            }
        }
        return true;
    }
}
