package com.danasea.backend.modules.order.presentation.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.order.application.OrderPaymentService;
import com.danasea.backend.modules.order.application.dtos.GetVendorOrdersQuery;
import com.danasea.backend.modules.order.application.dtos.SubOrderDetailResult;
import com.danasea.backend.modules.order.application.usecases.GetVendorOrdersUseCase;
import com.danasea.backend.modules.order.domain.models.OrderPagedResult;
import com.danasea.backend.modules.order.presentation.dtos.OrderPageResponse;
import com.danasea.backend.modules.order.presentation.dtos.SubOrderResponse;
import com.danasea.backend.security.infrastructure.SecurityUtils;

@RestController
@RequestMapping("/api/vendor/orders")
public class VendorOrderController {

    private final OrderPaymentService orderPaymentService;
    private final GetVendorOrdersUseCase getVendorOrdersUseCase;

    @Autowired
    public VendorOrderController(
            OrderPaymentService orderPaymentService,
            GetVendorOrdersUseCase getVendorOrdersUseCase) {
        this.orderPaymentService = orderPaymentService;
        this.getVendorOrdersUseCase = getVendorOrdersUseCase;
    }

    public VendorOrderController(OrderPaymentService orderPaymentService) {
        this(orderPaymentService, null);
    }

    @GetMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<OrderPageResponse<SubOrderResponse>> getVendorOrders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));

        if (getVendorOrdersUseCase != null) {
            OrderPagedResult<SubOrderDetailResult> paged = getVendorOrdersUseCase.execute(
                    new GetVendorOrdersQuery(userId, page, size));
            List<SubOrderResponse> items = paged.content().stream()
                    .map(sub -> new SubOrderResponse(
                            sub.id(),
                            sub.vendorId(),
                            sub.serviceId(),
                            sub.slotId(),
                            sub.quantity(),
                            sub.unitPrice(),
                            sub.subtotalAmount(),
                            sub.status()
                    ))
                    .toList();
            return ResponseEntity.ok(new OrderPageResponse<>(
                    items,
                    paged.page(),
                    paged.size(),
                    paged.totalElements(),
                    paged.totalPages()
            ));
        }

        return ResponseEntity.ok(orderPaymentService.getVendorOrders(userId, page, size));
    }
}
