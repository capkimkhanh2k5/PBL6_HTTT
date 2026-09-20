package com.danasea.backend.modules.booking.domain.models;

import java.util.Collections;
import java.util.List;

public record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PagedResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PagedResult<>(
                content != null ? content : Collections.emptyList(),
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
