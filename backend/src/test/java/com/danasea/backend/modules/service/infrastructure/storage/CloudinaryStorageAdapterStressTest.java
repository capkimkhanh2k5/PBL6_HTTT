package com.danasea.backend.modules.service.infrastructure.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.danasea.backend.modules.service.domain.exceptions.FileStorageException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CloudinaryStorageAdapterStressTest {

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

    @Nested
    @DisplayName("1. Empirical Verification of Public ID Extraction & Edge Cases")
    class ExtractPublicIdEdgeCases {

        @Test
        @DisplayName("Should extract public ID from standard Cloudinary image URL")
        void standardImageUrl() {
            String url = "https://res.cloudinary.com/demo/image/upload/v1234567890/services/images/sample.jpg";
            assertEquals("services/images/sample", adapter.extractPublicId(url));
        }

        @Test
        @DisplayName("Should extract public ID from URL with short version (v1)")
        void shortVersionUrl() {
            String url = "https://res.cloudinary.com/demo/image/upload/v1/services/images/sample.jpg";
            assertEquals("services/images/sample", adapter.extractPublicId(url));
        }

        @Test
        @DisplayName("Should extract public ID from URL without version")
        void urlWithoutVersion() {
            String url = "https://res.cloudinary.com/demo/image/upload/services/images/sample.jpg";
            assertEquals("services/images/sample", adapter.extractPublicId(url));
        }

        @Test
        @DisplayName("Should extract public ID from URL with transformation before version")
        void urlWithTransformation() {
            String url = "https://res.cloudinary.com/demo/image/upload/c_fill,w_300,h_200/v1612345678/services/images/sample.png";
            assertEquals("services/images/sample", adapter.extractPublicId(url));
        }

        @Test
        @DisplayName("Should handle multiple nested folders")
        void deeplyNestedFolders() {
            String url = "https://res.cloudinary.com/demo/image/upload/v123/a/b/c/d/e/f/g/h/i/j/k/image.webp";
            assertEquals("a/b/c/d/e/f/g/h/i/j/k/image", adapter.extractPublicId(url));
        }

        @Test
        @DisplayName("Should handle URL with multiple dots in filename")
        void multipleDotsInFilename() {
            String url = "https://res.cloudinary.com/demo/image/upload/v1234567890/services/docs/cert.signed.v2.pdf";
            assertEquals("services/docs/cert.signed.v2", adapter.extractPublicId(url));
        }

        @Test
        @DisplayName("Should return raw public ID when input is not an HTTP URL")
        void rawPublicId() {
            assertEquals("services/images/my-photo", adapter.extractPublicId("services/images/my-photo"));
            assertEquals("my-photo", adapter.extractPublicId("my-photo"));
        }

        @Test
        @DisplayName("Should handle URL without /upload/ keyword")
        void urlWithoutUploadKeyword() {
            String url = "https://other-cdn.com/assets/images/photo.jpg";
            assertEquals("https://other-cdn.com/assets/images/photo.jpg", adapter.extractPublicId(url));
        }

        @Test
        @DisplayName("Should return null for null or blank input")
        void nullOrBlankInput() {
            assertNull(adapter.extractPublicId(null));
            assertNull(adapter.extractPublicId(""));
            assertNull(adapter.extractPublicId("   "));
        }

        @Test
        @DisplayName("Edge Case: URL with query parameters (e.g. ?v=123)")
        void urlWithQueryParameters() {
            String url = "https://res.cloudinary.com/demo/image/upload/v1234567890/services/images/photo.jpg?query=param";
            String publicId = adapter.extractPublicId(url);
            // Verify what extractPublicId returns
            assertNotNull(publicId);
            System.out.println("PublicId for query param URL: " + publicId);
        }

        @Test
        @DisplayName("Edge Case: URL with query parameter containing dots (e.g. ?version=1.2.0)")
        void urlWithDottedQueryParameters() {
            String url = "https://res.cloudinary.com/demo/image/upload/v1234567890/services/images/photo.jpg?version=1.2.0";
            String publicId = adapter.extractPublicId(url);
            System.out.println("PublicId for dotted query param URL: " + publicId);
        }

        @Test
        @DisplayName("Edge Case: Extremely long URL (3000+ chars)")
        void extremelyLongUrl() {
            StringBuilder sb = new StringBuilder("https://res.cloudinary.com/demo/image/upload/v1234567890/");
            for (int i = 0; i < 150; i++) {
                sb.append("long_folder_name_segment_");
            }
            sb.append("/photo.png");
            String longUrl = sb.toString();
            String publicId = adapter.extractPublicId(longUrl);
            assertNotNull(publicId);
            assertFalse(publicId.endsWith(".png"));
            assertTrue(publicId.endsWith("/photo"));
        }

        @Test
        @DisplayName("Edge Case: URL with folder named upload")
        void folderNamedUpload() {
            String url = "https://res.cloudinary.com/demo/image/upload/v1234567890/services/upload/photo.jpg";
            assertEquals("services/upload/photo", adapter.extractPublicId(url));
        }
    }

    @Nested
    @DisplayName("2. Empirical Verification of Upload File")
    class UploadFileEmpiricalTests {

        @Test
        @DisplayName("Upload with secure_url returned")
        void uploadSecureUrl() throws IOException {
            byte[] content = "valid image content".getBytes();
            String secureUrl = "https://res.cloudinary.com/demo/image/upload/v123/services/images/pic.png";
            when(uploader.upload(eq(content), anyMap())).thenReturn(Map.of("secure_url", secureUrl));

            String result = adapter.uploadFile(content, "pic.png", "services/images");
            assertEquals(secureUrl, result);
        }

        @Test
        @DisplayName("Upload fallback to url when secure_url is null")
        void uploadFallbackToUrl() throws IOException {
            byte[] content = "valid image content".getBytes();
            String plainUrl = "http://res.cloudinary.com/demo/image/upload/v123/services/images/pic.png";
            Map<String, Object> map = new HashMap<>();
            map.put("secure_url", null);
            map.put("url", plainUrl);
            when(uploader.upload(eq(content), anyMap())).thenReturn(map);

            String result = adapter.uploadFile(content, "pic.png", "services/images");
            assertEquals(plainUrl, result);
        }

        @Test
        @DisplayName("Upload with both secure_url and url null should throw FileStorageException")
        void uploadBothUrlsNull() throws IOException {
            byte[] content = "valid image content".getBytes();
            Map<String, Object> map = new HashMap<>();
            map.put("secure_url", null);
            map.put("url", null);
            when(uploader.upload(eq(content), anyMap())).thenReturn(map);

            FileStorageException ex = assertThrows(FileStorageException.class,
                    () -> adapter.uploadFile(content, "pic.png", "services/images"));
            assertTrue(ex.getMessage().contains("Cloudinary upload did not return a valid URL"));
        }

        @Test
        @DisplayName("Upload with unicode, spaces, and special chars in filename")
        void uploadFilenameSpecialChars() throws IOException {
            byte[] content = "dummy bytes".getBytes();
            String returnedUrl = "https://res.cloudinary.com/demo/image/upload/v123/hoso.pdf";
            when(uploader.upload(eq(content), anyMap())).thenReturn(Map.of("secure_url", returnedUrl));

            String unicodeFilename = "Hồ sơ bảo hiểm & kiểm định #1 (2026).pdf";
            String result = adapter.uploadFile(content, unicodeFilename, "services/safety_docs");
            assertEquals(returnedUrl, result);

            verify(uploader).upload(eq(content), argThat(params ->
                    Boolean.TRUE.equals(params.get("use_filename")) &&
                    Boolean.TRUE.equals(params.get("unique_filename")) &&
                    "services/safety_docs".equals(params.get("folder")) &&
                    "auto".equals(params.get("resource_type"))
            ));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Upload with blank or empty folder should not include folder param")
        void uploadBlankFolder(String folder) throws IOException {
            byte[] content = "dummy bytes".getBytes();
            when(uploader.upload(eq(content), anyMap())).thenReturn(Map.of("secure_url", "https://url.com/a.jpg"));

            adapter.uploadFile(content, "sample.jpg", folder);

            verify(uploader).upload(eq(content), argThat(params -> !params.containsKey("folder")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Upload with blank or empty filename should not include filename params")
        void uploadBlankFilename(String filename) throws IOException {
            byte[] content = "dummy bytes".getBytes();
            when(uploader.upload(eq(content), anyMap())).thenReturn(Map.of("secure_url", "https://url.com/a.jpg"));

            adapter.uploadFile(content, filename, "folder");

            verify(uploader).upload(eq(content), argThat(params ->
                    !params.containsKey("use_filename") && !params.containsKey("unique_filename")));
        }
    }

    @Nested
    @DisplayName("3. Empirical Verification of Delete File")
    class DeleteFileEmpiricalTests {

        @Test
        @DisplayName("Delete raw resource type should pass resource_type=raw and invalidate=true")
        void deleteRawResource() throws IOException {
            String rawUrl = "https://res.cloudinary.com/demo/raw/upload/v1234567890/services/safety_docs/cert.pdf";
            when(uploader.destroy(eq("services/safety_docs/cert"), anyMap())).thenReturn(Map.of("result", "ok"));

            adapter.deleteFile(rawUrl);

            verify(uploader).destroy(eq("services/safety_docs/cert"), argThat(params ->
                    "raw".equals(params.get("resource_type")) && Boolean.TRUE.equals(params.get("invalidate"))
            ));
        }

        @Test
        @DisplayName("Delete image resource type should not pass resource_type=raw")
        void deleteImageResource() throws IOException {
            String imageUrl = "https://res.cloudinary.com/demo/image/upload/v1234567890/services/images/pic.png";
            when(uploader.destroy(eq("services/images/pic"), anyMap())).thenReturn(Map.of("result", "ok"));

            adapter.deleteFile(imageUrl);

            verify(uploader).destroy(eq("services/images/pic"), argThat(params ->
                    !params.containsKey("resource_type") && Boolean.TRUE.equals(params.get("invalidate"))
            ));
        }

        @Test
        @DisplayName("Delete with null/blank does nothing and catches no exception")
        void deleteNullOrBlank() throws IOException {
            adapter.deleteFile(null);
            adapter.deleteFile("");
            adapter.deleteFile("   ");

            verify(uploader, never()).destroy(any(), any());
        }

        @Test
        @DisplayName("Delete should throw FileStorageException when destroy throws Exception")
        void deleteFailureWrapsException() throws IOException {
            String url = "https://res.cloudinary.com/demo/image/upload/v1/services/images/pic.jpg";
            when(uploader.destroy(any(), anyMap())).thenThrow(new IOException("Connection reset"));

            FileStorageException ex = assertThrows(FileStorageException.class, () -> adapter.deleteFile(url));
            assertTrue(ex.getMessage().contains("Connection reset"));
        }
    }
}
