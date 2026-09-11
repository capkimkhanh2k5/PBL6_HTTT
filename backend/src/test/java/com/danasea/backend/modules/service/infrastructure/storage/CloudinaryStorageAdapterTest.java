package com.danasea.backend.modules.service.infrastructure.storage;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.danasea.backend.modules.service.domain.exceptions.FileStorageException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CloudinaryStorageAdapterTest {

    private Cloudinary cloudinary;
    private Uploader uploader;
    private CloudinaryStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        adapter = new CloudinaryStorageAdapter(cloudinary);
    }

    @Test
    void shouldUploadFileSuccessfully() throws IOException {
        byte[] fileData = "test content".getBytes();
        String secureUrl = "https://res.cloudinary.com/demo/image/upload/v1234567890/services/images/photo.jpg";

        when(uploader.upload(eq(fileData), anyMap())).thenReturn(Map.of("secure_url", secureUrl));

        String result = adapter.uploadFile(fileData, "photo.jpg", "services/images");

        assertEquals(secureUrl, result);
        verify(uploader).upload(eq(fileData), argThat(params ->
                "services/images".equals(params.get("folder")) &&
                "auto".equals(params.get("resource_type"))
        ));
    }

    @Test
    void shouldFallbackToUrlWhenSecureUrlMissing() throws IOException {
        byte[] fileData = "test content".getBytes();
        String regularUrl = "http://res.cloudinary.com/demo/image/upload/v1234567890/services/images/photo.jpg";

        when(uploader.upload(eq(fileData), anyMap())).thenReturn(Map.of("url", regularUrl));

        String result = adapter.uploadFile(fileData, "photo.jpg", "services/images");

        assertEquals(regularUrl, result);
    }

    @Test
    void shouldThrowExceptionWhenUploadingEmptyOrNullFile() {
        assertThrows(FileStorageException.class, () -> adapter.uploadFile(null, "file.jpg", "folder"));
        assertThrows(FileStorageException.class, () -> adapter.uploadFile(new byte[0], "file.jpg", "folder"));
    }

    @Test
    void shouldThrowFileStorageExceptionWhenUploadFails() throws IOException {
        byte[] fileData = "test content".getBytes();
        when(uploader.upload(eq(fileData), anyMap())).thenThrow(new RuntimeException("Cloudinary API unavailable"));

        FileStorageException ex = assertThrows(FileStorageException.class,
                () -> adapter.uploadFile(fileData, "photo.jpg", "services/images"));

        assertTrue(ex.getMessage().contains("Cloudinary API unavailable"));
    }

    @Test
    void shouldThrowFileStorageExceptionWhenNoUrlReturned() throws IOException {
        byte[] fileData = "test content".getBytes();
        when(uploader.upload(eq(fileData), anyMap())).thenReturn(Map.of());

        assertThrows(FileStorageException.class,
                () -> adapter.uploadFile(fileData, "photo.jpg", "services/images"));
    }

    @Test
    void shouldDeleteFileSuccessfully() throws IOException {
        String fileUrl = "https://res.cloudinary.com/demo/image/upload/v1234567890/services/images/sample.jpg";

        when(uploader.destroy(eq("services/images/sample"), anyMap())).thenReturn(Map.of("result", "ok"));

        adapter.deleteFile(fileUrl);

        verify(uploader).destroy(eq("services/images/sample"), argThat(params ->
                Boolean.TRUE.equals(params.get("invalidate"))
        ));
    }

    @Test
    void shouldDeleteRawFileWithRawResourceType() throws IOException {
        String fileUrl = "https://res.cloudinary.com/demo/raw/upload/v1234567890/services/docs/cert.pdf";

        when(uploader.destroy(eq("services/docs/cert"), anyMap())).thenReturn(Map.of("result", "ok"));

        adapter.deleteFile(fileUrl);

        verify(uploader).destroy(eq("services/docs/cert"), argThat(params ->
                "raw".equals(params.get("resource_type")) && Boolean.TRUE.equals(params.get("invalidate"))
        ));
    }

    @Test
    void shouldDoNothingWhenDeleteFileWithNullOrBlankUrl() throws IOException {
        adapter.deleteFile(null);
        adapter.deleteFile("");
        adapter.deleteFile("   ");

        verify(uploader, never()).destroy(any(), any());
    }

    @Test
    void shouldThrowFileStorageExceptionWhenDestroyFails() throws IOException {
        String fileUrl = "https://res.cloudinary.com/demo/image/upload/v1234567890/services/images/sample.jpg";
        when(uploader.destroy(any(), anyMap())).thenThrow(new RuntimeException("Cloudinary delete failed"));

        assertThrows(FileStorageException.class, () -> adapter.deleteFile(fileUrl));
    }

    @Test
    void shouldExtractPublicIdCorrectly() {
        assertEquals("sample", adapter.extractPublicId("sample"));
        assertEquals("services/images/photo", adapter.extractPublicId("https://res.cloudinary.com/demo/image/upload/v1234567890/services/images/photo.jpg"));
        assertEquals("services/docs/cert", adapter.extractPublicId("https://res.cloudinary.com/demo/raw/upload/v999/services/docs/cert.pdf"));
        assertEquals("photo", adapter.extractPublicId("https://res.cloudinary.com/demo/image/upload/photo.png"));
    }
}
