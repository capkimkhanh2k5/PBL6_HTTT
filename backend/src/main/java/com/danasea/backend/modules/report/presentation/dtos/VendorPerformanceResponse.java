package com.danasea.backend.modules.report.presentation.dtos;

import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorPerformanceItem;

import java.util.List;

public record VendorPerformanceResponse(List<VendorPerformanceItem> items, TimeRange timeRange) {

    public static VendorPerformanceResponse of(
            List<VendorPerformanceItem> items, TimeRange timeRange) {
        return new VendorPerformanceResponse(items != null ? items : List.of(), timeRange);
    }
}
