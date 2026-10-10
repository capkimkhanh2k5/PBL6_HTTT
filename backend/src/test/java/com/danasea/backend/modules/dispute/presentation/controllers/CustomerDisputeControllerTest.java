package com.danasea.backend.modules.dispute.presentation.controllers;

import com.danasea.backend.modules.dispute.application.usecases.CreateDisputeUseCase;
import com.danasea.backend.modules.dispute.application.usecases.GetCustomerDisputeDetailUseCase;
import com.danasea.backend.modules.dispute.application.usecases.GetCustomerDisputesUseCase;
import com.danasea.backend.modules.dispute.domain.exceptions.DisputeNotFoundException;
import com.danasea.backend.modules.dispute.domain.exceptions.UnauthorizedDisputeAccessException;
import com.danasea.backend.modules.dispute.domain.models.DisputeReason;
import com.danasea.backend.modules.dispute.domain.models.DisputeStatus;
import com.danasea.backend.modules.dispute.presentation.dtos.CreateDisputeRequest;
import com.danasea.backend.modules.dispute.presentation.dtos.DisputeResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerDisputeController Unit & MockMvc Tests")
class CustomerDisputeControllerTest {

    @Mock
    private CreateDisputeUseCase createDisputeUseCase;

    @Mock
    private GetCustomerDisputesUseCase getCustomerDisputesUseCase;

    @Mock
    private GetCustomerDisputeDetailUseCase getCustomerDisputeDetailUseCase;

    @InjectMocks
    private CustomerDisputeController customerDisputeController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID orderId;
    private UUID subOrderId;
    private UUID disputeId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        subOrderId = UUID.randomUUID();
        disputeId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        DisputeExceptionHandler exceptionHandler = new DisputeExceptionHandler();
        mockMvc = MockMvcBuilders.standaloneSetup(customerDisputeController)
                .setControllerAdvice(exceptionHandler)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("createDispute delegates to usecase and returns 201 CREATED")
    void createDispute_Success201() {
        CreateDisputeRequest request = new CreateDisputeRequest(
                subOrderId, DisputeReason.SERVICE_NOT_AS_DESCRIBED, "Test description", List.of("url1"));

        DisputeResponse expectedResponse = new DisputeResponse(
                disputeId, orderId, subOrderId, customerId,
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

    @Test
    @DisplayName("GET /api/disputes returns 200 OK with paginated customer disputes")
    void getCustomerDisputes_Success200() throws Exception {
        DisputeResponse dispute = new DisputeResponse(
                disputeId, orderId, subOrderId, customerId,
                DisputeReason.SERVICE_NOT_AS_DESCRIBED, "Not as described",
                List.of("http://example.com/e1.jpg"), DisputeStatus.OPEN,
                null, null, null, null,
                OffsetDateTime.now(), OffsetDateTime.now(),
                "Vendor response text", List.of("http://example.com/v1.jpg"), OffsetDateTime.now()
        );

        when(getCustomerDisputesUseCase.execute(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dispute), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/disputes")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(disputeId.toString()))
                .andExpect(jsonPath("$.content[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].vendorResponse").value("Vendor response text"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(getCustomerDisputesUseCase).execute(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/disputes/{id} returns 200 OK with dispute detail")
    void getCustomerDisputeDetail_Success200() throws Exception {
        DisputeResponse dispute = new DisputeResponse(
                disputeId, orderId, subOrderId, customerId,
                DisputeReason.VENDOR_NO_SHOW, "Vendor didn't reply",
                List.of("http://example.com/e1.png"), DisputeStatus.UNDER_REVIEW,
                null, null, null, null,
                OffsetDateTime.now(), OffsetDateTime.now(),
                "Here is our explanation", List.of("http://example.com/counter.png"), OffsetDateTime.now()
        );

        when(getCustomerDisputeDetailUseCase.execute(disputeId)).thenReturn(dispute);

        mockMvc.perform(get("/api/disputes/{id}", disputeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(disputeId.toString()))
                .andExpect(jsonPath("$.reason").value("VENDOR_NO_SHOW"))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.vendorResponse").value("Here is our explanation"));

        verify(getCustomerDisputeDetailUseCase).execute(disputeId);
    }

    @Test
    @DisplayName("GET /api/disputes/{id} with unauthorized access returns 403 Forbidden")
    void getCustomerDisputeDetail_UnauthorizedAccess_Returns403() throws Exception {
        when(getCustomerDisputeDetailUseCase.execute(disputeId))
                .thenThrow(new UnauthorizedDisputeAccessException("User does not have permission to access this dispute"));

        mockMvc.perform(get("/api/disputes/{id}", disputeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verify(getCustomerDisputeDetailUseCase).execute(disputeId);
    }

    @Test
    @DisplayName("GET /api/disputes/{id} with non-existent id returns 404 Not Found")
    void getCustomerDisputeDetail_NotFound_Returns404() throws Exception {
        when(getCustomerDisputeDetailUseCase.execute(disputeId))
                .thenThrow(new DisputeNotFoundException("Dispute not found with id: " + disputeId));

        mockMvc.perform(get("/api/disputes/{id}", disputeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DISPUTE_NOT_FOUND"));

        verify(getCustomerDisputeDetailUseCase).execute(disputeId);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/disputes creates dispute and returns 201 CREATED via MockMvc")
    void createDispute_ViaMockMvc_Returns201() throws Exception {
        CreateDisputeRequest request = new CreateDisputeRequest(
                subOrderId, DisputeReason.SAFETY_CONCERN, "Dangerous environment", List.of("http://example.com/proof.jpg"));

        DisputeResponse expectedResponse = new DisputeResponse(
                disputeId, orderId, subOrderId, customerId,
                DisputeReason.SAFETY_CONCERN, "Dangerous environment",
                List.of("http://example.com/proof.jpg"), DisputeStatus.OPEN,
                null, null, null, null,
                OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(createDisputeUseCase.execute(eq(orderId), any(CreateDisputeRequest.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/orders/{id}/disputes", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(disputeId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(createDisputeUseCase).execute(eq(orderId), any(CreateDisputeRequest.class));
    }
}
