package com.danasea.backend.modules.vendor.application.usecases;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.vendor.domain.exceptions.VendorNotFoundException;
import com.danasea.backend.modules.vendor.domain.models.BadgeTier;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.domain.models.VerificationStatus;
import com.danasea.backend.modules.vendor.infrastructure.mappers.VendorMapper;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateVendorProfileUseCaseTest {

    @Mock
    private JpaVendorRepository jpaVendorRepository;

    private VendorMapper vendorMapper;

    private UpdateVendorProfileUseCase updateVendorProfileUseCase;

    private UUID userId;
    private UUID vendorId;
    private VendorJpaEntity existingVendorEntity;

    @BeforeEach
    void setUp() {
        vendorMapper = new VendorMapper();
        updateVendorProfileUseCase = new UpdateVendorProfileUseCase(
                jpaVendorRepository,
                vendorMapper
        );

        userId = UUID.randomUUID();
        vendorId = UUID.randomUUID();

        existingVendorEntity = new VendorJpaEntity();
        existingVendorEntity.setId(vendorId);
        existingVendorEntity.setUserId(userId);
        existingVendorEntity.setBusinessName("Original Seafood Market");
        existingVendorEntity.setTaxCode("0111222333");
        existingVendorEntity.setAddress("100 Bach Dang, Da Nang");
        existingVendorEntity.setBankAccountNumber("111122223333");
        existingVendorEntity.setBankName("Techcombank");
        existingVendorEntity.setBankAccountHolder("NGUYEN VAN ORIG");
        existingVendorEntity.setVerificationStatus(VerificationStatus.APPROVED);
        existingVendorEntity.setBadgeTier(BadgeTier.TOP_RATED);
        existingVendorEntity.setRatingAvg(new BigDecimal("4.85"));
        existingVendorEntity.setRatingCount(42);
    }

    @Test
    @DisplayName("Should successfully update all valid vendor profile fields")
    void shouldUpdateVendorProfileSuccessfully() {
        when(jpaVendorRepository.findByUserId(userId)).thenReturn(Optional.of(existingVendorEntity));
        when(jpaVendorRepository.save(any(VendorJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateVendorProfileCommand command = UpdateVendorProfileCommand.builder()
                .businessName("Updated Seafood Superstore")
                .taxCode("0999888777")
                .address("200 Hoang Sa, Da Nang")
                .bankAccountNumber("999988887777")
                .bankName("VietinBank")
                .bankAccountHolder("NGUYEN VAN UPDATED")
                .build();

        Vendor updatedVendor = updateVendorProfileUseCase.execute(userId, command);

        assertNotNull(updatedVendor);
        assertEquals(vendorId, updatedVendor.getId());
        assertEquals(userId, updatedVendor.getUserId());
        assertEquals("Updated Seafood Superstore", updatedVendor.getBusinessName());
        assertEquals("0999888777", updatedVendor.getTaxCode());
        assertEquals("200 Hoang Sa, Da Nang", updatedVendor.getAddress());
        assertEquals("999988887777", updatedVendor.getBankAccountNumber());
        assertEquals("VietinBank", updatedVendor.getBankName());
        assertEquals("NGUYEN VAN UPDATED", updatedVendor.getBankAccountHolder());

        ArgumentCaptor<VendorJpaEntity> entityCaptor = ArgumentCaptor.forClass(VendorJpaEntity.class);
        verify(jpaVendorRepository).save(entityCaptor.capture());
        VendorJpaEntity saved = entityCaptor.getValue();
        assertEquals("Updated Seafood Superstore", saved.getBusinessName());
        assertEquals("0999888777", saved.getTaxCode());
        assertEquals("200 Hoang Sa, Da Nang", saved.getAddress());
        assertEquals("999988887777", saved.getBankAccountNumber());
        assertEquals("VietinBank", saved.getBankName());
        assertEquals("NGUYEN VAN UPDATED", saved.getBankAccountHolder());
    }

    @Test
    @DisplayName("Should only update non-null fields during partial update, preserving other existing fields")
    void shouldOnlyUpdateNonNullFieldsInPartialUpdate() {
        when(jpaVendorRepository.findByUserId(userId)).thenReturn(Optional.of(existingVendorEntity));
        when(jpaVendorRepository.save(any(VendorJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateVendorProfileCommand command = UpdateVendorProfileCommand.builder()
                .businessName("New Name Only")
                .address("New Address Only")
                .build();

        Vendor updatedVendor = updateVendorProfileUseCase.execute(userId, command);

        assertNotNull(updatedVendor);
        assertEquals("New Name Only", updatedVendor.getBusinessName());
        assertEquals("New Address Only", updatedVendor.getAddress());
        // Other fields must remain unchanged
        assertEquals("0111222333", updatedVendor.getTaxCode());
        assertEquals("111122223333", updatedVendor.getBankAccountNumber());
        assertEquals("Techcombank", updatedVendor.getBankName());
        assertEquals("NGUYEN VAN ORIG", updatedVendor.getBankAccountHolder());
    }

    @Test
    @DisplayName("Should structurally prevent mass assignment: verificationStatus, ratingAvg, ratingCount, and badgeTier remain untouched")
    void shouldPreventMassAssignmentOfSensitiveFields() {
        when(jpaVendorRepository.findByUserId(userId)).thenReturn(Optional.of(existingVendorEntity));
        when(jpaVendorRepository.save(any(VendorJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateVendorProfileCommand command = UpdateVendorProfileCommand.builder()
                .businessName("Tamper Attempt Business")
                .build();

        Vendor result = updateVendorProfileUseCase.execute(userId, command);

        // Verification of immutable security-sensitive fields
        assertEquals(VerificationStatus.APPROVED, result.getVerificationStatus());
        assertEquals(BadgeTier.TOP_RATED, result.getBadgeTier());
        assertEquals(new BigDecimal("4.85"), result.getRatingAvg());
        assertEquals(42, result.getRatingCount());

        ArgumentCaptor<VendorJpaEntity> entityCaptor = ArgumentCaptor.forClass(VendorJpaEntity.class);
        verify(jpaVendorRepository).save(entityCaptor.capture());
        VendorJpaEntity savedEntity = entityCaptor.getValue();
        assertEquals(VerificationStatus.APPROVED, savedEntity.getVerificationStatus(), "VerificationStatus must never be altered via update");
        assertEquals(BadgeTier.TOP_RATED, savedEntity.getBadgeTier(), "BadgeTier must never be altered via update");
        assertEquals(new BigDecimal("4.85"), savedEntity.getRatingAvg(), "RatingAvg must never be altered via update");
        assertEquals(42, savedEntity.getRatingCount(), "RatingCount must never be altered via update");
    }

    @Test
    @DisplayName("Should prevent cross-vendor manipulation: strictly queries by authenticated userId and throws VendorNotFoundException when not found")
    void shouldPreventCrossVendorManipulationWhenVendorNotFound() {
        UUID foreignUserId = UUID.randomUUID();
        when(jpaVendorRepository.findByUserId(foreignUserId)).thenReturn(Optional.empty());

        UpdateVendorProfileCommand command = UpdateVendorProfileCommand.builder()
                .businessName("Malicious Update")
                .build();

        assertThrows(VendorNotFoundException.class, () ->
                updateVendorProfileUseCase.execute(foreignUserId, command));

        verify(jpaVendorRepository).findByUserId(foreignUserId);
        verify(jpaVendorRepository, never()).findByUserId(userId);
        verify(jpaVendorRepository, never()).save(any(VendorJpaEntity.class));
    }

    @Test
    @DisplayName("Should strictly bind vendor lookup to authenticated userId")
    void shouldStrictlyBindVendorLookupToAuthenticatedUserId() {
        when(jpaVendorRepository.findByUserId(userId)).thenReturn(Optional.of(existingVendorEntity));
        when(jpaVendorRepository.save(any(VendorJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateVendorProfileCommand command = UpdateVendorProfileCommand.builder()
                .businessName("Legitimate Update")
                .build();

        updateVendorProfileUseCase.execute(userId, command);

        verify(jpaVendorRepository).findByUserId(userId);
        verify(jpaVendorRepository).save(existingVendorEntity);
    }

    @Test
    @DisplayName("Should preserve existing entity unchanged when command is null")
    void shouldPreserveEntityWhenCommandIsNull() {
        when(jpaVendorRepository.findByUserId(userId)).thenReturn(Optional.of(existingVendorEntity));
        when(jpaVendorRepository.save(any(VendorJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vendor result = updateVendorProfileUseCase.execute(userId, null);

        assertNotNull(result);
        assertEquals("Original Seafood Market", result.getBusinessName());
        assertEquals("0111222333", result.getTaxCode());
        verify(jpaVendorRepository).save(existingVendorEntity);
    }
}
