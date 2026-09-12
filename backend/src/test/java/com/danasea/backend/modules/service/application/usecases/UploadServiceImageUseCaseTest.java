package com.danasea.backend.modules.service.application.usecases;

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
import com.danasea.backend.modules.service.domain.exceptions.InvalidFileTypeException;
import com.danasea.backend.modules.service.domain.exceptions.MaxImagesExceededException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.UnauthorizedServiceAccessException;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceImageRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceImageUseCaseTest {

    @Mock
    private JpaServiceRepository serviceRepository;

    @Mock
    private JpaServiceImageRepository serviceImageRepository;

    @Mock
    private FileStoragePort fileStoragePort;

    private UploadServiceImageUseCase useCase;

    private UUID serviceId;
    private UUID vendorId;
    private ServiceJpaEntity serviceEntity;

    @BeforeEach
    void setUp() {
        useCase = new UploadServiceImageUseCase(serviceRepository, serviceImageRepository, fileStoragePort);

        serviceId = UUID.randomUUID();
        vendorId = UUID.randomUUID();

        serviceEntity = new ServiceJpaEntity();
        serviceEntity.setId(serviceId);
        serviceEntity.setVendorId(vendorId);
        serviceEntity.setName("Tour Lặn Ngắm San Hô");
    }

    @Test
    @DisplayName("AC1.1: Upload thành công ảnh đầu tiên với sort_order = 1")
    void shouldUploadSuccessfullyAsFirstImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.jpg", "image/jpeg", "image-bytes".getBytes()
        );
        String uploadedUrl = "https://res.cloudinary.com/danasea/image/upload/v1/services/cover.jpg";

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceImageRepository.countByServiceId(serviceId)).thenReturn(0L);
        when(serviceImageRepository.findMaxSortOrderByServiceId(serviceId)).thenReturn(Optional.empty());
        when(fileStoragePort.uploadFile(any(), eq("cover.jpg"), anyString())).thenReturn(uploadedUrl);
        when(serviceImageRepository.save(any(ServiceImageJpaEntity.class))).thenAnswer(invocation -> {
            ServiceImageJpaEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ServiceImageJpaEntity result = useCase.execute(serviceId, vendorId, file);

        assertNotNull(result);
        assertEquals(serviceId, result.getServiceId());
        assertEquals(uploadedUrl, result.getUrl());
        assertEquals((short) 1, result.getSortOrder());

        verify(serviceRepository).findById(serviceId);
        verify(serviceImageRepository).countByServiceId(serviceId);
        verify(fileStoragePort).uploadFile(file.getBytes(), "cover.jpg", "services/" + serviceId + "/images");
        verify(serviceImageRepository).save(any(ServiceImageJpaEntity.class));
    }

    @Test
    @DisplayName("AC1.2: Upload thành công và tự động tăng sort_order khi đã có ảnh trước đó")
    void shouldUploadSuccessfullyAndAutoIncrementSortOrder() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "gallery_photo.png", "image/png", "valid-png-content".getBytes()
        );
        String uploadedUrl = "https://res.cloudinary.com/danasea/image/upload/v1/services/gallery_photo.png";

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceImageRepository.countByServiceId(serviceId)).thenReturn(3L);
        when(serviceImageRepository.findMaxSortOrderByServiceId(serviceId)).thenReturn(Optional.of((short) 3));
        when(fileStoragePort.uploadFile(any(), eq("gallery_photo.png"), anyString())).thenReturn(uploadedUrl);
        when(serviceImageRepository.save(any(ServiceImageJpaEntity.class))).thenAnswer(invocation -> {
            ServiceImageJpaEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ServiceImageJpaEntity result = useCase.execute(serviceId, vendorId, file);

        assertNotNull(result);
        assertEquals(serviceId, result.getServiceId());
        assertEquals(uploadedUrl, result.getUrl());
        assertEquals((short) 4, result.getSortOrder());

        verify(serviceImageRepository).save(any(ServiceImageJpaEntity.class));
    }

    @Test
    @DisplayName("AC1.3: Chặn upload khi vượt quá số lượng 10 ảnh tối đa")
    void shouldThrowWhenExceedsMaxImagesLimit() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "extra.webp", "image/webp", "image-content".getBytes()
        );

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(serviceImageRepository.countByServiceId(serviceId)).thenReturn(10L);

        assertThrows(MaxImagesExceededException.class, () ->
                useCase.execute(serviceId, vendorId, file)
        );

        verify(fileStoragePort, never()).uploadFile(any(), anyString(), anyString());
        verify(serviceImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC1.4: Ném ngoại lệ khi định dạng file không hợp lệ (PDF, EXE, etc.)")
    void shouldThrowWhenInvalidFileType() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "contract.pdf", "application/pdf", "pdf-content".getBytes()
        );

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));

        assertThrows(InvalidFileTypeException.class, () ->
                useCase.execute(serviceId, vendorId, invalidFile)
        );

        verifyNoInteractions(fileStoragePort);
        verify(serviceImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC1.5: Ném ngoại lệ khi file rỗng")
    void shouldThrowWhenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));

        assertThrows(InvalidFileTypeException.class, () ->
                useCase.execute(serviceId, vendorId, emptyFile)
        );

        verifyNoInteractions(fileStoragePort);
        verify(serviceImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC1.6: Ném ngoại lệ khi không tìm thấy Service (HTTP 404)")
    void shouldThrowWhenServiceNotFound() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes()
        );

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        assertThrows(ServiceNotFoundException.class, () ->
                useCase.execute(serviceId, vendorId, file)
        );

        verifyNoInteractions(fileStoragePort);
        verifyNoInteractions(serviceImageRepository);
    }

    @Test
    @DisplayName("AC1.7: Ném ngoại lệ khi Vendor không sở hữu Service (HTTP 403 / IDOR Protection)")
    void shouldThrowWhenNotServiceOwner() {
        UUID otherVendorId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes()
        );

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));

        assertThrows(UnauthorizedServiceAccessException.class, () ->
                useCase.execute(serviceId, otherVendorId, file)
        );

        verifyNoInteractions(fileStoragePort);
        verify(serviceImageRepository, never()).save(any());
    }
}
