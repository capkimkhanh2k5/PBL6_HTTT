package com.danasea.backend.modules.vendor.application.usecase;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.danasea.backend.modules.vendor.application.port.DocumentStoragePort;
import com.danasea.backend.modules.vendor.domain.exception.InvalidDocTypeException;
import com.danasea.backend.modules.vendor.domain.exception.VendorNotFoundException;
import com.danasea.backend.modules.vendor.domain.models.DocStatus;
import com.danasea.backend.modules.vendor.domain.models.DocType;
import com.danasea.backend.modules.vendor.domain.models.VendorDocument;
import com.danasea.backend.modules.vendor.infrastructure.mapper.VendorDocumentMapper;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorDocumentJpaEntity;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorDocumentRepository;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadVendorDocumentUseCaseTest {

    @Mock
    private JpaVendorRepository jpaVendorRepository;

    @Mock
    private JpaVendorDocumentRepository jpaVendorDocumentRepository;

    @Mock
    private DocumentStoragePort documentStoragePort;

    private VendorDocumentMapper vendorDocumentMapper;

    private UploadVendorDocumentUseCase uploadVendorDocumentUseCase;

    private UUID userId;
    private UUID vendorId;
    private VendorJpaEntity vendorEntity;
    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        vendorDocumentMapper = new VendorDocumentMapper();
        uploadVendorDocumentUseCase = new UploadVendorDocumentUseCase(
                jpaVendorRepository,
                jpaVendorDocumentRepository,
                documentStoragePort,
                vendorDocumentMapper
        );

        userId = UUID.randomUUID();
        vendorId = UUID.randomUUID();

        vendorEntity = new VendorJpaEntity();
        vendorEntity.setId(vendorId);
        vendorEntity.setUserId(userId);

        validFile = new MockMultipartFile(
                "file",
                "business_license.pdf",
                "application/pdf",
                "sample-content".getBytes()
        );
    }

    @Test
    @DisplayName("Should successfully upload BUSINESS_LICENSE document with PENDING status and null reviewedBy/reviewedAt")
    void shouldUploadBusinessLicenseDocumentSuccessfully() {
        when(jpaVendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendorEntity));
        when(documentStoragePort.uploadDocument(eq(validFile), eq("vendor_documents")))
                .thenReturn("https://cloudinary.com/danasea/docs/business_license.pdf");

        UUID generatedDocId = UUID.randomUUID();
        when(jpaVendorDocumentRepository.save(any(VendorDocumentJpaEntity.class))).thenAnswer(invocation -> {
            VendorDocumentJpaEntity entity = invocation.getArgument(0);
            entity.setId(generatedDocId);
            return entity;
        });

        VendorDocument result = uploadVendorDocumentUseCase.execute(userId, validFile, "BUSINESS_LICENSE");

        assertNotNull(result);
        assertEquals(generatedDocId, result.getId());
        assertEquals(vendorId, result.getVendorId());
        assertEquals(DocType.BUSINESS_LICENSE, result.getDocType());
        assertEquals("https://cloudinary.com/danasea/docs/business_license.pdf", result.getFileUrl());
        assertEquals(DocStatus.PENDING, result.getStatus());
        assertNull(result.getReviewedBy());
        assertNull(result.getReviewedAt());

        ArgumentCaptor<VendorDocumentJpaEntity> entityCaptor = ArgumentCaptor.forClass(VendorDocumentJpaEntity.class);
        verify(jpaVendorDocumentRepository).save(entityCaptor.capture());
        VendorDocumentJpaEntity savedEntity = entityCaptor.getValue();
        assertEquals(vendorId, savedEntity.getVendorId());
        assertEquals(DocType.BUSINESS_LICENSE, savedEntity.getDocType());
        assertEquals("https://cloudinary.com/danasea/docs/business_license.pdf", savedEntity.getFileUrl());
        assertEquals(DocStatus.PENDING, savedEntity.getStatus());
        assertNull(savedEntity.getReviewedBy());
        assertNull(savedEntity.getReviewedAt());
    }

    @Test
    @DisplayName("Should successfully upload SAFETY_CERT document with case-insensitive doc_type string")
    void shouldUploadSafetyCertDocumentSuccessfully() {
        when(jpaVendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendorEntity));
        when(documentStoragePort.uploadDocument(eq(validFile), eq("vendor_documents")))
                .thenReturn("https://cloudinary.com/danasea/docs/safety_cert.pdf");
        when(jpaVendorDocumentRepository.save(any(VendorDocumentJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VendorDocument result = uploadVendorDocumentUseCase.execute(userId, validFile, "safety_cert");

        assertNotNull(result);
        assertEquals(DocType.SAFETY_CERT, result.getDocType());
        assertEquals(DocStatus.PENDING, result.getStatus());
    }

    @Test
    @DisplayName("Should throw InvalidDocTypeException when doc_type is invalid string")
    void shouldThrowInvalidDocTypeExceptionWhenDocTypeIsInvalidString() {
        assertThrows(InvalidDocTypeException.class, () ->
                uploadVendorDocumentUseCase.execute(userId, validFile, "INVALID_DOC_TYPE"));

        verify(documentStoragePort, never()).uploadDocument(any(), any());
        verify(jpaVendorDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidDocTypeException when doc_type string is null")
    void shouldThrowInvalidDocTypeExceptionWhenDocTypeStringIsNull() {
        assertThrows(InvalidDocTypeException.class, () ->
                uploadVendorDocumentUseCase.execute(userId, validFile, (String) null));

        verify(documentStoragePort, never()).uploadDocument(any(), any());
        verify(jpaVendorDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidDocTypeException when doc_type string is blank")
    void shouldThrowInvalidDocTypeExceptionWhenDocTypeStringIsBlank() {
        assertThrows(InvalidDocTypeException.class, () ->
                uploadVendorDocumentUseCase.execute(userId, validFile, "   "));

        verify(documentStoragePort, never()).uploadDocument(any(), any());
        verify(jpaVendorDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidDocTypeException when DocType enum is null")
    void shouldThrowInvalidDocTypeExceptionWhenDocTypeEnumIsNull() {
        assertThrows(InvalidDocTypeException.class, () ->
                uploadVendorDocumentUseCase.execute(userId, validFile, (DocType) null));

        verify(documentStoragePort, never()).uploadDocument(any(), any());
        verify(jpaVendorDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw VendorNotFoundException when vendor profile does not exist for the user (upload before registration)")
    void shouldThrowVendorNotFoundExceptionWhenVendorDoesNotExist() {
        when(jpaVendorRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(VendorNotFoundException.class, () ->
                uploadVendorDocumentUseCase.execute(userId, validFile, "BUSINESS_LICENSE"));

        verify(documentStoragePort, never()).uploadDocument(any(), any());
        verify(jpaVendorDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when file is null")
    void shouldThrowIllegalArgumentExceptionWhenFileIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                uploadVendorDocumentUseCase.execute(userId, null, "BUSINESS_LICENSE"));

        verify(jpaVendorRepository, never()).findByUserId(any());
        verify(documentStoragePort, never()).uploadDocument(any(), any());
        verify(jpaVendorDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when file is empty")
    void shouldThrowIllegalArgumentExceptionWhenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(IllegalArgumentException.class, () ->
                uploadVendorDocumentUseCase.execute(userId, emptyFile, "BUSINESS_LICENSE"));

        verify(jpaVendorRepository, never()).findByUserId(any());
        verify(documentStoragePort, never()).uploadDocument(any(), any());
        verify(jpaVendorDocumentRepository, never()).save(any());
    }
}
