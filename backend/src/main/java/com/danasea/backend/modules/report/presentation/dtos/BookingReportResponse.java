package com.danasea.backend.modules.report.presentation.dtos;

import com.danasea.backend.modules.report.domain.models.BookingReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;

import java.util.List;

public record BookingReportResponse(List<BookingReportItem> items, TimeRange timeRange) {

    public static BookingReportResponse of(List<BookingReportItem> items, TimeRange timeRange) {
        return new BookingReportResponse(items != null ? items : List.of(), timeRange);
    }
}
