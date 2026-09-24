package com.danasea.backend.modules.order.presentation.dtos;

import java.util.List;

public record OrderPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
