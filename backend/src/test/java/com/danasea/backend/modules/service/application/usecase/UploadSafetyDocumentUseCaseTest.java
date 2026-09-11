package com.danasea.backend.modules.service.application.usecase;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.danasea.backend.modules.service.application.ports.FileStoragePort;
import com.danasea.backend.modules.service.domain.models.DocStatus;
import com.danasea.backend.modules.service.domain.exceptions.InvalidFileTypeException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.UnauthorizedServiceAccessException;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSafetyDocumentJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSafetyDocumentRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadSafetyDocumentUseCaseTest {

    @Mock
    private JpaServiceRepository serviceRepository;

    @Mock
    private JpaServiceSafetyDocumentRepository safetyDocumentRepository;

    @Mock
    private FileStoragePort fileStoragePort;

    private UploadSafetyDocumentUseCase useCase;

    private UUID serviceId;
    private UUID vendorId;
    private ServiceJpaEntity serviceEntity;

    @BeforeEach
    void setUp() {
        useCase = new UploadSafetyDocumentUseCase(serviceRepository, safetyDocumentRepository, fileStoragePort);

        serviceId = UUID.randomUUID();
        vendorId = UUID.randomUUID();

        serviceEntity = new ServiceJpaEntity();
        serviceEntity.setId(serviceId);
        serviceEntity.setVendorId(vendorId);
        serviceEntity.setName("Tour Lặn Biển Sâu");
    }

    @Test
    @DisplayName("AC3.1: Upload chứng chỉ an toàn (PDF) thành công với trạng thái PENDING và audit fields null")
    void shouldUploadSafetyDocumentSuccessfullyWithPendingStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "padi_certificate.pdf", "application/pdf", "dummy-pdf-content".getBytes()
        );
        String uploadedUrl = "https://res.cloudinary.com/danasea/raw/upload/v1/services/padi_certificate.pdf";

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(fileStoragePort.uploadFile(any(), eq("padi_certificate.pdf"), anyString())).thenReturn(uploadedUrl);
        when(safetyDocumentRepository.save(any(ServiceSafetyDocumentJpaEntity.class))).thenAnswer(invocation -> {
            ServiceSafetyDocumentJpaEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ServiceSafetyDocumentJpaEntity result = useCase.execute(serviceId, vendorId, file);

        assertNotNull(result);
        assertEquals(serviceId, result.getServiceId());
        assertEquals(uploadedUrl, result.getFileUrl());
        assertEquals(DocStatus.PENDING, result.getStatus());
        assertNull(result.getReviewedBy());
        assertNull(result.getReviewedAt());
        assertNull(result.getRejectionReason());

        verify(serviceRepository).findById(serviceId);
        verify(fileStoragePort).uploadFile(file.getBytes(), "padi_certificate.pdf", "services/" + serviceId + "/documents");
        verify(safetyDocumentRepository).save(any(ServiceSafetyDocumentJpaEntity.class));
    }

    @Test
    @DisplayName("AC3.2: Upload chứng chỉ an toàn dạng ảnh (JPG/PNG) hợp lệ thành công")
    void shouldUploadSafetyDocumentAsImageSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "license.png", "image/png", "png-image-content".getBytes()
        );
        String uploadedUrl = "https://res.cloudinary.com/danasea/image/upload/v1/services/license.png";

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(fileStoragePort.uploadFile(any(), eq("license.png"), anyString())).thenReturn(uploadedUrl);
        when(safetyDocumentRepository.save(any(ServiceSafetyDocumentJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceSafetyDocumentJpaEntity result = useCase.execute(serviceId, vendorId, file);

        assertNotNull(result);
        assertEquals(DocStatus.PENDING, result.getStatus());
        verify(safetyDocumentRepository).save(any(ServiceSafetyDocumentJpaEntity.class));
    }

    @Test
    @DisplayName("AC3.3: Chặn file định dạng không được hỗ trợ (EXE, TXT, ZIP)")
    void shouldThrowWhenInvalidFileTypeProvided() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "document.txt", "text/plain", "plain-text".getBytes()
        );

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));

        assertThrows(InvalidFileTypeException.class, () ->
                useCase.execute(serviceId, vendorId, invalidFile)
        );

        verifyNoInteractions(fileStoragePort);
        verify(safetyDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC3.4: Chặn khi Vendor không sở hữu Service (HTTP 403 / IDOR)")
    void shouldThrowWhenNotServiceOwner() {
        UUID intruderVendorId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "cert.pdf", "application/pdf", "pdf-data".getBytes()
        );

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));

        assertThrows(UnauthorizedServiceAccessException.class, () ->
                useCase.execute(serviceId, intruderVendorId, file)
        );

        verifyNoInteractions(fileStoragePort);
        verify(safetyDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC3.5: Chặn khi không tìm thấy Service (HTTP 404)")
    void shouldThrowWhenServiceNotFound() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cert.pdf", "application/pdf", "pdf-data".getBytes()
        );

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        assertThrows(ServiceNotFoundException.class, () ->
                useCase.execute(serviceId, vendorId, file)
        );

        verifyNoInteractions(fileStoragePort);
        verifyNoInteractions(safetyDocumentRepository);
    }
}
