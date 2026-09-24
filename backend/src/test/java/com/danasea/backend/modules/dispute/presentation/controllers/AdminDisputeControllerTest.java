package com.danasea.backend.modules.dispute.presentation.controllers;

import com.danasea.backend.modules.dispute.application.usecases.GetDisputesUseCase;
import com.danasea.backend.modules.dispute.application.usecases.ResolveDisputeUseCase;
import com.danasea.backend.modules.dispute.domain.models.DisputeReason;
import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
import com.danasea.backend.modules.dispute.presentation.dtos.ResolveDisputeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDisputeController Unit Tests")
class AdminDisputeControllerTest {

    @Mock
    private GetDisputesUseCase getDisputesUseCase;

    @Mock
    private ResolveDisputeUseCase resolveDisputeUseCase;

    @InjectMocks
    private AdminDisputeController adminDisputeController;

    private UUID disputeId;

    @BeforeEach
    void setUp() {
        disputeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getDisputes delegates to usecase and returns 200 OK")
    void getDisputes_Success200() {
        Pageable pageable = PageRequest.of(0, 10);
        DisputeResponse dispute = new DisputeResponse(
                disputeId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                DisputeReason.SAFETY_CONCERN, "Safety issue", List.of(), DisputeStatus.OPEN,
                null, null, null, null, OffsetDateTime.now(), OffsetDateTime.now()
        );
        Page<DisputeResponse> page = new PageImpl<>(List.of(dispute), pageable, 1);

        when(getDisputesUseCase.execute(DisputeStatus.OPEN, DisputeReason.SAFETY_CONCERN, pageable))
                .thenReturn(page);

        ResponseEntity<Page<DisputeResponse>> response = adminDisputeController.getDisputes(
                DisputeStatus.OPEN, DisputeReason.SAFETY_CONCERN, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
        verify(getDisputesUseCase).execute(DisputeStatus.OPEN, DisputeReason.SAFETY_CONCERN, pageable);
    }

    @Test
    @DisplayName("resolveDispute delegates to usecase and returns 200 OK")
    void resolveDispute_Success200() {
        ResolveDisputeRequest request = new ResolveDisputeRequest(
                DisputeStatus.RESOLVED_REFUND, new BigDecimal("100.0"), "Full refund approved");

        DisputeResponse resolvedResponse = new DisputeResponse(
                disputeId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                DisputeReason.SERVICE_NOT_AS_DESCRIBED, "Description", List.of(),
                DisputeStatus.RESOLVED_REFUND, new BigDecimal("100.0"), "Full refund approved",
                UUID.randomUUID(), OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(resolveDisputeUseCase.execute(disputeId, request)).thenReturn(resolvedResponse);

        ResponseEntity<DisputeResponse> response = adminDisputeController.resolveDispute(disputeId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resolvedResponse);
        verify(resolveDisputeUseCase).execute(disputeId, request);
    }
}
