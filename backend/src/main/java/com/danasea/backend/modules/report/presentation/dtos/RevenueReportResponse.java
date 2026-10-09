package com.danasea.backend.modules.report.presentation.dtos;

import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;

import java.util.List;

public record RevenueReportResponse(List<RevenueReportItem> items, TimeRange timeRange) {

    public static RevenueReportResponse of(List<RevenueReportItem> items, TimeRange timeRange) {
        return new RevenueReportResponse(items != null ? items : List.of(), timeRange);
    }
}
