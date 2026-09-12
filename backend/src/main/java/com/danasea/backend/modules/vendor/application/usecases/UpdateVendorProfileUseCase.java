package com.danasea.backend.modules.vendor.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.vendor.domain.exceptions.VendorNotFoundException;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import com.danasea.backend.modules.vendor.infrastructure.mappers.VendorMapper;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorJpaEntity;
import com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateVendorProfileUseCase {

    private final JpaVendorRepository jpaVendorRepository;
    private final VendorMapper vendorMapper;

    @Transactional
    public Vendor execute(UUID userId, UpdateVendorProfileCommand command) {
        VendorJpaEntity entity = jpaVendorRepository.findByUserId(userId)
                .orElseThrow(VendorNotFoundException::new);

        if (command != null) {
            if (command.businessName() != null) {
                entity.setBusinessName(command.businessName());
            }
            if (command.taxCode() != null) {
                entity.setTaxCode(command.taxCode());
            }
            if (command.address() != null) {
                entity.setAddress(command.address());
            }
            if (command.bankAccountNumber() != null) {
                entity.setBankAccountNumber(command.bankAccountNumber());
            }
            if (command.bankName() != null) {
                entity.setBankName(command.bankName());
            }
            if (command.bankAccountHolder() != null) {
                entity.setBankAccountHolder(command.bankAccountHolder());
            }
        }

        VendorJpaEntity savedEntity = jpaVendorRepository.save(entity);
        return vendorMapper.toDomain(savedEntity);
    }
}
