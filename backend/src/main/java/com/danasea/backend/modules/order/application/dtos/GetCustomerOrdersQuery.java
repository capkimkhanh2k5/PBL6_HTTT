package com.danasea.backend.modules.order.application.dtos;

import java.util.UUID;

public record GetCustomerOrdersQuery(
        UUID customerId,
        int page,
        int size
) {}
