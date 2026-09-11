package com.danasea.backend.modules.vendor.infrastructure.mapper;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.vendor.domain.models.VendorDocument;
import com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorDocumentJpaEntity;

@Component
public class VendorDocumentMapper {

    public VendorDocument toDomain(VendorDocumentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        VendorDocument domain = new VendorDocument();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setVendorId(entity.getVendorId());
        domain.setDocType(entity.getDocType());
        domain.setFileUrl(entity.getFileUrl());
        domain.setStatus(entity.getStatus());
        domain.setReviewedBy(entity.getReviewedBy());
        domain.setReviewedAt(entity.getReviewedAt());
        return domain;
    }

    public VendorDocumentJpaEntity toEntity(VendorDocument domain) {
        if (domain == null) {
            return null;
        }
        VendorDocumentJpaEntity entity = new VendorDocumentJpaEntity();
        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVendorId(domain.getVendorId());
        entity.setDocType(domain.getDocType());
        entity.setFileUrl(domain.getFileUrl());
        entity.setStatus(domain.getStatus());
        entity.setReviewedBy(domain.getReviewedBy());
        entity.setReviewedAt(domain.getReviewedAt());
        return entity;
    }
}
