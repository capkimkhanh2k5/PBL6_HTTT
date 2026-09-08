package com.danasea.backend.modules.order.domain.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Invoice extends BaseDomainModel {
    private UUID masterOrderId;
    private String invoiceNumber;
    private String pdfUrl;
    private OffsetDateTime issuedAt;
}
