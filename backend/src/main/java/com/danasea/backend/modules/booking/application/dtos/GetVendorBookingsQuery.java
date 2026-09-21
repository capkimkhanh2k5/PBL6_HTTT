package com.danasea.backend.modules.booking.application.dtos;

import java.util.UUID;

import com.danasea.backend.modules.booking.domain.models.BookingStatus;

public record GetVendorBookingsQuery(
        UUID userId,
        BookingStatus status,
        int page,
        int size,
        String sortBy,
        String sortDirection
) {}
