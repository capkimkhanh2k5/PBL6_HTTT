package com.danasea.backend.security.authorization;

import com.danasea.backend.security.authentication.application.port.UserAccountPort;
import com.danasea.backend.security.authentication.domain.model.Authentication;
import com.danasea.backend.security.authentication.infrastructure.security.CustomAuthenticationEntryPoint;
import com.danasea.backend.security.authentication.infrastructure.security.JwtTokenProvider;
import com.danasea.backend.security.authorization.application.port.AuthorizationPort;
import com.danasea.backend.security.authorization.domain.model.AuthorizationSubject;
import com.danasea.backend.security.authorization.infrastructure.security.CustomAccessDeniedHandler;
import com.danasea.backend.shared.presentation.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for CustomAccessDeniedHandler and CustomAuthenticationEntryPoint.
 * Verifies that:
 * 1. 403 Forbidden responses return application/json ErrorResponse shape, NOT HTML.
 * 2. 401 Unauthorized responses return application/json ErrorResponse shape, NOT HTML.
 * 3. Both MockMvc integration tests and direct component unit tests confirm correct behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AccessDeniedHandlerTest extends BaseSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @MockitoBean
    private UserAccountPort userAccountPort;

    @MockitoBean
    private AuthorizationPort authorizationPort;

    @Nested
    @DisplayName("MockMvc HTTP Integration Tests")
    class HttpIntegrationTests {

        @Test
        @DisplayName("Authenticated user with wrong role receives 403 JSON ErrorResponse, NOT HTML")
        void wrongRole_returns403JsonErrorResponseNotHtml() throws Exception {
            String email = "unauthorized_customer@example.com";
            UUID userId = UUID.randomUUID();
            Authentication auth = new Authentication(userId, email, "hash", "CUSTOMER", true, true);
            String token = jwtTokenProvider.generateAccessToken(auth);

            when(userAccountPort.findByEmail(email)).thenReturn(Optional.of(auth));
            when(authorizationPort.findSubjectByEmail(email))
                    .thenReturn(new AuthorizationSubject(userId, email, Set.of("CUSTOMER"), Set.of()));

            MvcResult result = mockMvc.perform(get("/api/admin/dashboard")
                    .header("Authorization", "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();

            // Verify body is NOT HTML
            assertThat(responseBody).doesNotContain("<html");
            assertThat(responseBody).doesNotContain("<!DOCTYPE");
            assertThat(responseBody).doesNotContain("<body");

            // Verify body strictly deserializes to ErrorResponse record
            ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
            assertThat(errorResponse).isNotNull();
            assertThat(errorResponse.code()).isEqualTo("ACCESS_DENIED");
            assertThat(errorResponse.message()).isNotBlank();
        }

        @Test
        @DisplayName("Unauthenticated request receives 401 JSON ErrorResponse, NOT HTML")
        void unauthenticated_returns401JsonErrorResponseNotHtml() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/admin/dashboard")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();

            // Verify body is NOT HTML
            assertThat(responseBody).doesNotContain("<html");
            assertThat(responseBody).doesNotContain("<!DOCTYPE");
            assertThat(responseBody).doesNotContain("<body");

            // Verify body strictly deserializes to ErrorResponse record
            ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
            assertThat(errorResponse).isNotNull();
            assertThat(errorResponse.code()).isEqualTo("UNAUTHORIZED");
            assertThat(errorResponse.message()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Handler Component Unit Tests")
    class ComponentUnitTests {

        @Test
        @DisplayName("CustomAccessDeniedHandler writes HTTP 403 with ErrorResponse JSON")
        void customAccessDeniedHandler_serializesErrorResponse() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AccessDeniedException exception = new AccessDeniedException("Access denied: insufficient permissions");

            accessDeniedHandler.handle(request, response, exception);

            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
            assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");

            ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
            assertThat(errorResponse.code()).isEqualTo("ACCESS_DENIED");
            assertThat(errorResponse.message()).isEqualTo("Access denied: insufficient permissions");
        }

        @Test
        @DisplayName("CustomAccessDeniedHandler handles null exception message with default message")
        void customAccessDeniedHandler_handlesNullExceptionMessage() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AccessDeniedException exception = new AccessDeniedException(null);

            accessDeniedHandler.handle(request, response, exception);

            assertThat(response.getStatus()).isEqualTo(403);
            ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
            assertThat(errorResponse.code()).isEqualTo("ACCESS_DENIED");
            assertThat(errorResponse.message()).isEqualTo("Access denied");
        }

        @Test
        @DisplayName("CustomAuthenticationEntryPoint writes HTTP 401 with ErrorResponse JSON")
        void customAuthenticationEntryPoint_serializesErrorResponse() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            InsufficientAuthenticationException exception = new InsufficientAuthenticationException("Full authentication is required");

            authenticationEntryPoint.commence(request, response, exception);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
            assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");

            ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
            assertThat(errorResponse.code()).isEqualTo("UNAUTHORIZED");
            assertThat(errorResponse.message()).isEqualTo("Full authentication is required");
        }

        @Test
        @DisplayName("CustomAuthenticationEntryPoint handles null exception message with default message")
        void customAuthenticationEntryPoint_handlesNullExceptionMessage() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            BadCredentialsException exception = new BadCredentialsException(null);

            authenticationEntryPoint.commence(request, response, exception);

            assertThat(response.getStatus()).isEqualTo(401);
            ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
            assertThat(errorResponse.code()).isEqualTo("UNAUTHORIZED");
            assertThat(errorResponse.message()).isEqualTo("Full authentication is required to access this resource");
        }
    }
}
