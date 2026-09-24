package com.danasea.backend.modules.dispute.presentation.controllers;

import com.danasea.backend.modules.dispute.application.usecases.CreateDisputeUseCase;
import com.danasea.backend.modules.dispute.domain.models.DisputeReason;
import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;
import com.danasea.backend.modules.dispute.presentation.dtos.CreateDisputeRequest;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerDisputeController Unit Tests")
class CustomerDisputeControllerTest {

    @Mock
    private CreateDisputeUseCase createDisputeUseCase;

    @InjectMocks
    private CustomerDisputeController customerDisputeController;

    private UUID orderId;
    private UUID subOrderId;
    private UUID disputeId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        subOrderId = UUID.randomUUID();
        disputeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("createDispute delegates to usecase and returns 201 CREATED")
    void createDispute_Success201() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                subOrderId, DisputeReason.SERVICE_NOT_AS_DESCRIBED, "Test description", List.of("url1"));

        DisputeResponse expectedResponse = new DisputeResponse(
                disputeId, orderId, subOrderId, UUID.randomUUID(),
                DisputeReason.SERVICE_NOT_AS_DESCRIBED, "Test description",
                List.of("url1"), DisputeStatus.OPEN, null, null, null, null,
                OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(createDisputeUseCase.execute(orderId, request)).thenReturn(expectedResponse);

        ResponseEntity<DisputeResponse> response = customerDisputeController.createDispute(orderId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(createDisputeUseCase).execute(orderId, request);
    }
}
