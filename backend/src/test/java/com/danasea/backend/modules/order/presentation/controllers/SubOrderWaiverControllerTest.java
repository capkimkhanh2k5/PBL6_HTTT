package com.danasea.backend.modules.order.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.danasea.backend.modules.order.application.dtos.AcceptSubOrderWaiverCommand;
import com.danasea.backend.modules.order.application.dtos.SubOrderWaiverAcceptanceResult;
import com.danasea.backend.modules.order.application.usecases.AcceptSubOrderWaiverUseCase;
import com.danasea.backend.modules.order.domain.exceptions.WaiverVersionMismatchException;
import com.danasea.backend.modules.order.presentation.dtos.SubOrderWaiverAcceptanceRequest;
import com.danasea.backend.modules.order.presentation.dtos.SubOrderWaiverAcceptanceResponse;
import com.danasea.backend.modules.order.presentation.handlers.OrderExceptionHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubOrderWaiverController Presentation Tests")
class SubOrderWaiverControllerTest {

    @Mock private AcceptSubOrderWaiverUseCase acceptSubOrderWaiverUseCase;

    @InjectMocks private SubOrderWaiverController controller;

    private MockMvc mockMvc;

    private final UUID customerId = UUID.randomUUID();
    private final UUID masterOrderId = UUID.randomUUID();
    private final UUID subOrderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new OrderExceptionHandler())
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityUser(UUID userId, String... roles) {
        var authorities = Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        var auth =
                new UsernamePasswordAuthenticationToken(
                        userId.toString(), "credentials", authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Nested
    @DisplayName("Direct Method Invocations")
    class DirectMethodTests {

        @Test
        @DisplayName("200 OK: Chủ đơn xác nhận cam kết thành công")
        void acceptWaiver_Success() {
            mockSecurityUser(customerId, "ROLE_CUSTOMER");
            OffsetDateTime acceptedAt = OffsetDateTime.now();

            when(acceptSubOrderWaiverUseCase.execute(
                            eq(customerId), eq(subOrderId), any(AcceptSubOrderWaiverCommand.class)))
                    .thenReturn(
                            new SubOrderWaiverAcceptanceResult(
                                    subOrderId, masterOrderId, true, 1, "VI", acceptedAt));

            SubOrderWaiverAcceptanceRequest request =
                    new SubOrderWaiverAcceptanceRequest(true, 1, "VI");
            ResponseEntity<SubOrderWaiverAcceptanceResponse> response =
                    controller.acceptWaiver(subOrderId, request);

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            SubOrderWaiverAcceptanceResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.subOrderId()).isEqualTo(subOrderId);
            assertThat(body.masterOrderId()).isEqualTo(masterOrderId);
            assertThat(body.accepted()).isTrue();
            assertThat(body.waiverVersion()).isEqualTo(1);
            assertThat(body.language()).isEqualTo("VI");
            assertThat(body.acceptedAt()).isEqualTo(acceptedAt);
        }

        @Test
        @DisplayName("401/403: Chưa xác thực danh tính ném AccessDeniedException")
        void acceptWaiver_Unauthenticated_Throws() {
            SubOrderWaiverAcceptanceRequest request =
                    new SubOrderWaiverAcceptanceRequest(true, 1, "VI");

            assertThatThrownBy(() -> controller.acceptWaiver(subOrderId, request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("not authenticated");
        }
    }

    @Nested
    @DisplayName("MockMvc HTTP Endpoints & Exception Handling")
    class MockMvcTests {

        @Test
        @DisplayName("POST /api/sub-orders/{id}/waiver-acceptance thành công -> 200 OK")
        void httpPostAcceptWaiver_Success() throws Exception {
            mockSecurityUser(customerId, "ROLE_CUSTOMER");
            OffsetDateTime acceptedAt = OffsetDateTime.now();

            when(acceptSubOrderWaiverUseCase.execute(
                            eq(customerId), eq(subOrderId), any(AcceptSubOrderWaiverCommand.class)))
                    .thenReturn(
                            new SubOrderWaiverAcceptanceResult(
                                    subOrderId, masterOrderId, true, 1, "VI", acceptedAt));

            String requestJson =
                    """
                    {
                        "accepted": true,
                        "version": 1,
                        "language": "VI"
                    }
                    """;

            mockMvc.perform(
                            post("/api/sub-orders/{id}/waiver-acceptance", subOrderId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subOrderId").value(subOrderId.toString()))
                    .andExpect(jsonPath("$.masterOrderId").value(masterOrderId.toString()))
                    .andExpect(jsonPath("$.accepted").value(true))
                    .andExpect(jsonPath("$.waiverVersion").value(1))
                    .andExpect(jsonPath("$.language").value("VI"));
        }

        @Test
        @DisplayName(
                "POST /api/sub-orders/{id}/waiver-acceptance với accepted=false -> 400 Bad Request")
        void httpPostAcceptWaiver_AcceptedFalse_BadRequest() throws Exception {
            mockSecurityUser(customerId, "ROLE_CUSTOMER");

            String requestJson =
                    """
                    {
                        "accepted": false,
                        "version": 1,
                        "language": "VI"
                    }
                    """;

            mockMvc.perform(
                            post("/api/sub-orders/{id}/waiver-acceptance", subOrderId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName(
                "POST /api/sub-orders/{id}/waiver-acceptance khi sai version -> 409 Conflict với"
                        + " WAIVER_VERSION_MISMATCH")
        void httpPostAcceptWaiver_VersionMismatch_Conflict() throws Exception {
            mockSecurityUser(customerId, "ROLE_CUSTOMER");

            when(acceptSubOrderWaiverUseCase.execute(
                            eq(customerId), eq(subOrderId), any(AcceptSubOrderWaiverCommand.class)))
                    .thenThrow(new WaiverVersionMismatchException("Waiver version mismatch"));

            String requestJson =
                    """
                    {
                        "accepted": true,
                        "version": 1,
                        "language": "VI"
                    }
                    """;

            mockMvc.perform(
                            post("/api/sub-orders/{id}/waiver-acceptance", subOrderId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("WAIVER_VERSION_MISMATCH"));
        }
    }
}
