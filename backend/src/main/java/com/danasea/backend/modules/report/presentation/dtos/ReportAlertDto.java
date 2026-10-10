package com.danasea.backend.modules.report.presentation.dtos;

import com.danasea.backend.modules.report.domain.enums.VendorAlertSeverity;
import com.danasea.backend.modules.report.domain.enums.VendorIssueType;
import com.danasea.backend.modules.report.domain.models.VendorDiagnosisAlert;

import java.util.UUID;

public record ReportAlertDto(
        UUID vendorId,
        String businessName,
        VendorIssueType issueType,
        VendorAlertSeverity severity,
        String message,
        double metricValue) {

    public static ReportAlertDto from(VendorDiagnosisAlert alert) {
        if (alert == null) {
            return null;
        }
        return new ReportAlertDto(
                alert.getVendorId(),
                alert.getBusinessName(),
                alert.getIssueType(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.getMetricValue());
    }
}
