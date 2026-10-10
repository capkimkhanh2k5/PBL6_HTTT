package com.danasea.backend.modules.dispute.presentation.controllers;

import com.danasea.backend.modules.dispute.application.usecases.GetVendorDisputeDetailUseCase;
import com.danasea.backend.modules.dispute.application.usecases.GetVendorDisputesUseCase;
import com.danasea.backend.modules.dispute.application.usecases.SubmitVendorDisputeResponseUseCase;
import com.danasea.backend.modules.dispute.domain.exceptions.DisputeAlreadyResolvedException;
import com.danasea.backend.modules.dispute.domain.exceptions.DisputeNotFoundException;
import com.danasea.backend.modules.dispute.domain.exceptions.UnauthorizedDisputeAccessException;
import com.danasea.backend.modules.dispute.domain.models.DisputeReason;
import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
import com.danasea.backend.modules.dispute.presentation.dtos.SubmitVendorResponseRequest;
import com.danasea.backend.modules.dispute.presentation.handlers.DisputeExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("VendorDisputeController MockMvc Tests")
class VendorDisputeControllerTest {

    @Mock
    private GetVendorDisputesUseCase getVendorDisputesUseCase;

    @Mock
    private GetVendorDisputeDetailUseCase getVendorDisputeDetailUseCase;

    @Mock
    private SubmitVendorDisputeResponseUseCase submitVendorDisputeResponseUseCase;

    @InjectMocks
    private VendorDisputeController vendorDisputeController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID disputeId;
    private UUID orderId;
    private UUID subOrderId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        disputeId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        subOrderId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        DisputeExceptionHandler exceptionHandler = new DisputeExceptionHandler();
        mockMvc = MockMvcBuilders.standaloneSetup(vendorDisputeController)
                .setControllerAdvice(exceptionHandler)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/vendor/disputes returns 200 OK with vendor's disputes")
    void getVendorDisputes_Success200() throws Exception {
        DisputeResponse dispute = new DisputeResponse(
                disputeId, orderId, subOrderId, customerId,
                DisputeReason.SERVICE_NOT_AS_DESCRIBED, "Customer complaint",
                List.of("http://example.com/c1.jpg"), DisputeStatus.OPEN,
                null, null, null, null,
                OffsetDateTime.now(), OffsetDateTime.now(),
                null, null, null
        );

        when(getVendorDisputesUseCase.execute(eq(DisputeStatus.OPEN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dispute), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/vendor/disputes")
                        .param("status", "OPEN")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(disputeId.toString()))
                .andExpect(jsonPath("$.content[0].subOrderId").value(subOrderId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(getVendorDisputesUseCase).execute(eq(DisputeStatus.OPEN), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/vendor/disputes/{id} returns 200 OK with dispute detail")
    void getVendorDisputeDetail_Success200() throws Exception {
        DisputeResponse dispute = new DisputeResponse(
                disputeId, orderId, subOrderId, customerId,
                DisputeReason.VENDOR_NO_SHOW, "Customer claimed vendor was late",
                List.of("http://example.com/c1.png"), DisputeStatus.OPEN,
                null, null, null, null,
                OffsetDateTime.now(), OffsetDateTime.now(),
                null, null, null
        );

        when(getVendorDisputeDetailUseCase.execute(disputeId)).thenReturn(dispute);

        mockMvc.perform(get("/api/vendor/disputes/{id}", disputeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(disputeId.toString()))
                .andExpect(jsonPath("$.subOrderId").value(subOrderId.toString()))
                .andExpect(jsonPath("$.reason").value("VENDOR_NO_SHOW"));

        verify(getVendorDisputeDetailUseCase).execute(disputeId);
    }

    @Test
    @DisplayName("GET /api/vendor/disputes/{id} with unauthorized sub-order returns 403 Forbidden")
    void getVendorDisputeDetail_UnauthorizedAccess_Returns403() throws Exception {
        when(getVendorDisputeDetailUseCase.execute(disputeId))
                .thenThrow(new UnauthorizedDisputeAccessException("User does not have permission to access this dispute"));

        mockMvc.perform(get("/api/vendor/disputes/{id}", disputeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verify(getVendorDisputeDetailUseCase).execute(disputeId);
    }

    @Test
    @DisplayName("GET /api/vendor/disputes/{id} with non-existent id returns 404 Not Found")
    void getVendorDisputeDetail_NotFound_Returns404() throws Exception {
        when(getVendorDisputeDetailUseCase.execute(disputeId))
                .thenThrow(new DisputeNotFoundException("Dispute not found with id: " + disputeId));

        mockMvc.perform(get("/api/vendor/disputes/{id}", disputeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DISPUTE_NOT_FOUND"));

        verify(getVendorDisputeDetailUseCase).execute(disputeId);
    }

    @Test
    @DisplayName("POST /api/vendor/disputes/{id}/responses returns 200 OK with updated dispute")
    void submitVendorResponse_Success200() throws Exception {
        SubmitVendorResponseRequest request = new SubmitVendorResponseRequest(
                "We arrived on time and provided full service.",
                List.of("http://example.com/v_proof1.jpg", "http://example.com/v_proof2.pdf")
        );

        DisputeResponse updatedDispute = new DisputeResponse(
                disputeId, orderId, subOrderId, customerId,
                DisputeReason.VENDOR_NO_SHOW, "Customer claimed vendor was late",
                List.of("http://example.com/c1.png"), DisputeStatus.UNDER_REVIEW,
                null, null, null, null,
                OffsetDateTime.now(), OffsetDateTime.now(),
                "We arrived on time and provided full service.",
                List.of("http://example.com/v_proof1.jpg", "http://example.com/v_proof2.pdf"),
                OffsetDateTime.now()
        );

        when(submitVendorDisputeResponseUseCase.execute(eq(disputeId), any(SubmitVendorResponseRequest.class)))
                .thenReturn(updatedDispute);

        mockMvc.perform(post("/api/vendor/disputes/{id}/responses", disputeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(disputeId.toString()))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.vendorResponse").value("We arrived on time and provided full service."))
                .andExpect(jsonPath("$.vendorEvidenceUrls.length()").value(2));

        verify(submitVendorDisputeResponseUseCase).execute(eq(disputeId), any(SubmitVendorResponseRequest.class));
    }

    @Test
    @DisplayName("POST /api/vendor/disputes/{id}/responses with blank response returns 400 Bad Request")
    void submitVendorResponse_BlankContent_Returns400() throws Exception {
        SubmitVendorResponseRequest request = new SubmitVendorResponseRequest("   ", List.of());

        mockMvc.perform(post("/api/vendor/disputes/{id}/responses", disputeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("POST /api/vendor/disputes/{id}/responses with unauthorized vendor returns 403 Forbidden")
    void submitVendorResponse_UnauthorizedAccess_Returns403() throws Exception {
        SubmitVendorResponseRequest request = new SubmitVendorResponseRequest(
                "Valid explanation", List.of("http://example.com/proof.jpg"));

        when(submitVendorDisputeResponseUseCase.execute(eq(disputeId), any(SubmitVendorResponseRequest.class)))
                .thenThrow(new UnauthorizedDisputeAccessException("User does not have permission to respond to this dispute"));

        mockMvc.perform(post("/api/vendor/disputes/{id}/responses", disputeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("POST /api/vendor/disputes/{id}/responses on already resolved dispute returns 409 Conflict")
    void submitVendorResponse_AlreadyResolved_Returns409() throws Exception {
        SubmitVendorResponseRequest request = new SubmitVendorResponseRequest(
                "Trying to respond late", List.of());

        when(submitVendorDisputeResponseUseCase.execute(eq(disputeId), any(SubmitVendorResponseRequest.class)))
                .thenThrow(new DisputeAlreadyResolvedException("Cannot submit response for an already resolved dispute"));

        mockMvc.perform(post("/api/vendor/disputes/{id}/responses", disputeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DISPUTE_ALREADY_RESOLVED"));
    }
}
