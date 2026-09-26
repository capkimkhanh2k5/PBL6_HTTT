package com.danasea.backend.modules.order.application.dtos;

import java.util.UUID;

public record GetVendorOrdersQuery(
        UUID vendorId,
        int page,
        int size
) {}
