package com.danasea.backend.modules.report.domain.models;

import com.danasea.backend.modules.report.domain.enums.VendorAlertSeverity;
import com.danasea.backend.modules.report.domain.enums.VendorIssueType;

import java.util.Objects;
import java.util.UUID;

/** Cảnh báo chẩn đoán hoạt động của Vendor hoặc dịch vụ. */
public final class VendorDiagnosisAlert {

    private final UUID vendorId;
    private final String businessName;
    private final VendorIssueType issueType;
    private final VendorAlertSeverity severity;
    private final String message;
    private final double metricValue;

    public VendorDiagnosisAlert(
            UUID vendorId,
            String businessName,
            VendorIssueType issueType,
            VendorAlertSeverity severity,
            String message,
            double metricValue) {
        this.vendorId = vendorId;
        this.businessName = businessName;
        this.issueType = Objects.requireNonNull(issueType, "issueType cannot be null");
        this.severity = Objects.requireNonNull(severity, "severity cannot be null");
        this.message = message != null ? message : "";
        this.metricValue = metricValue;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public VendorIssueType getIssueType() {
        return issueType;
    }

    public VendorAlertSeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public double getMetricValue() {
        return metricValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VendorDiagnosisAlert that = (VendorDiagnosisAlert) o;
        return Double.compare(that.metricValue, metricValue) == 0
                && Objects.equals(vendorId, that.vendorId)
                && Objects.equals(businessName, that.businessName)
                && issueType == that.issueType
                && severity == that.severity
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendorId, businessName, issueType, severity, message, metricValue);
    }

    @Override
    public String toString() {
        return "VendorDiagnosisAlert{"
                + "vendorId="
                + vendorId
                + ", businessName='"
                + businessName
                + '\''
                + ", issueType="
                + issueType
                + ", severity="
                + severity
                + ", message='"
                + message
                + '\''
                + ", metricValue="
                + metricValue
                + '}';
    }
}
