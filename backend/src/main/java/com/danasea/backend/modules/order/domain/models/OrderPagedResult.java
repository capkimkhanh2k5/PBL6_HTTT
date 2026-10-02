package com.danasea.backend.modules.order.domain.models;

import java.util.List;

public record OrderPagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
