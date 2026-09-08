package com.danasea.backend.modules.vendor.domain.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VendorDocument extends BaseDomainModel {
    private UUID vendorId;
    private DocType docType;
    private String fileUrl;
    private DocStatus status;
    private UUID reviewedBy;
    private OffsetDateTime reviewedAt;
}
