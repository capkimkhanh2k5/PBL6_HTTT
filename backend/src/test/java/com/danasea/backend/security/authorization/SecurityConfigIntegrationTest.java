package com.danasea.backend.security.authorization;

import com.danasea.backend.security.authentication.application.ports.UserAccountPort;
import com.danasea.backend.security.authentication.domain.models.Authentication;
import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;
import com.danasea.backend.security.authentication.infrastructure.security.JwtTokenProvider;
import com.danasea.backend.security.authorization.application.ports.AuthorizationPort;
import com.danasea.backend.security.authorization.domain.models.AuthorizationSubject;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test suite for Spring Security filter chain path-based RBAC enforcement.
 *
 * Requirements verified:
 * 1. Path-based authorization: /api/admin/** requires ROLE_ADMIN.
 * 2. CUSTOMER tokens are rejected with 403 Forbidden.
 * 3. VENDOR tokens are rejected with 403 Forbidden.
 * 4. ADMIN tokens are permitted through to the endpoint returning 200 OK.
 * 5. Unauthenticated requests without token return 401 Unauthorized.
 * 6. Requests with expired tokens return 401 Unauthorized.
 * 7. JWT stateless role drift edge case: When an admin's role is downgraded to CUSTOMER in DB,
 *    an active unexpired token minted with ADMIN claim is immediately rejected with 403 Forbidden.
 *
 * Security filter chain, JwtAuthenticationFilter, JwtTokenProvider, and error handlers are REAL.
 * Only domain ports (UserAccountPort, AuthorizationPort) are mocked via @MockitoBean.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigIntegrationTest extends BaseSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @MockitoBean
    private UserAccountPort userAccountPort;

    @MockitoBean
    private AuthorizationPort authorizationPort;

    private String generateValidToken(String email, String role) {
        Authentication auth = new Authentication(
                UUID.randomUUID(),
                email,
                "password_hash",
                role,
                true,
                true
        );
        return jwtTokenProvider.generateAccessToken(auth);
    }

    private String generateExpiredToken(String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(Date.from(now.minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(now.minus(1, ChronoUnit.HOURS)))
                .signWith(jwtProperties.secretKey())
                .compact();
    }

    /**
     * Scenario 1: CUSTOMER token calls /api/admin/dashboard -> 403 Forbidden
     */
    @Test
    @DisplayName("Scenario 1: CUSTOMER token calls /api/admin/dashboard -> 403 Forbidden")
    void customerTokenCallsAdminEndpoint_returns403Forbidden() throws Exception {
        String email = "customer@example.com";
        UUID userId = UUID.randomUUID();
        Authentication auth = new Authentication(userId, email, "hash", "CUSTOMER", true, true);
        String token = jwtTokenProvider.generateAccessToken(auth);

        when(userAccountPort.findByEmail(email)).thenReturn(Optional.of(auth));
        when(authorizationPort.findSubjectByEmail(email))
                .thenReturn(new AuthorizationSubject(userId, email, Set.of("CUSTOMER"), Set.of("PRODUCT_READ", "ORDER_READ")));

        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value(notNullValue()));
    }

    /**
     * Scenario 2: VENDOR token calls /api/admin/dashboard -> 403 Forbidden
     */
    @Test
    @DisplayName("Scenario 2: VENDOR token calls /api/admin/dashboard -> 403 Forbidden")
    void vendorTokenCallsAdminEndpoint_returns403Forbidden() throws Exception {
        String email = "vendor@example.com";
        UUID userId = UUID.randomUUID();
        Authentication auth = new Authentication(userId, email, "hash", "VENDOR", true, true);
        String token = jwtTokenProvider.generateAccessToken(auth);

        when(userAccountPort.findByEmail(email)).thenReturn(Optional.of(auth));
        when(authorizationPort.findSubjectByEmail(email))
                .thenReturn(new AuthorizationSubject(userId, email, Set.of("VENDOR"), Set.of("PRODUCT_CREATE", "PRODUCT_READ")));

        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value(notNullValue()));
    }

    /**
     * Scenario 3: ADMIN token calls /api/admin/dashboard -> 200 OK (status: ADMIN_ACCESS_GRANTED)
     */
    @Test
    @DisplayName("Scenario 3: ADMIN token calls /api/admin/dashboard -> 200 OK (ADMIN_ACCESS_GRANTED)")
    void adminTokenCallsAdminEndpoint_returns200Ok() throws Exception {
        String email = "admin@example.com";
        UUID userId = UUID.randomUUID();
        Authentication auth = new Authentication(userId, email, "hash", "ADMIN", true, true);
        String token = jwtTokenProvider.generateAccessToken(auth);

        when(userAccountPort.findByEmail(email)).thenReturn(Optional.of(auth));
        when(authorizationPort.findSubjectByEmail(email))
                .thenReturn(new AuthorizationSubject(userId, email, Set.of("ADMIN"), Set.of("USER_READ", "USER_UPDATE")));

        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ADMIN_ACCESS_GRANTED"));
    }

    /**
     * Scenario 4: No token calls /api/admin/dashboard -> 401 Unauthorized
     */
    @Test
    @DisplayName("Scenario 4: No token calls /api/admin/dashboard -> 401 Unauthorized")
    void noTokenCallsAdminEndpoint_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value(notNullValue()));
    }

    /**
     * Scenario 5: Expired token calls /api/admin/dashboard -> 401 Unauthorized
     */
    @Test
    @DisplayName("Scenario 5: Expired token calls /api/admin/dashboard -> 401 Unauthorized")
    void expiredTokenCallsAdminEndpoint_returns401Unauthorized() throws Exception {
        String email = "admin@example.com";
        String expiredToken = generateExpiredToken(email, "ADMIN");

        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value(notNullValue()));
    }

    /**
     * Scenario 6: JWT Stateless Role Drift Test.
     *
     * Scenario description:
     * - A user was issued a JWT access token containing the claim "role": "ADMIN".
     * - The token remains valid (signature intact, not expired).
     * - Subsequently, an administrator demotes this user in the database to role "CUSTOMER".
     *
     * Problem in purely stateless JWT systems:
     * - If role permissions were resolved solely from JWT claims, the demoted user would retain
     *   full administrative privileges until the token expires (up to 15 minutes or longer).
     *
     * Danasea Architecture & Enforcement:
     * - JwtAuthenticationFilter does NOT rely on the informational "role" claim in the JWT.
     * - Instead, the filter extracts the user's email identity and performs a dynamic lookup via
     *   AuthorizationPort.findSubjectByEmail() to retrieve the user's actual database-persisted roles.
     * - When the user is demoted to CUSTOMER in the database, the filter assigns ROLE_CUSTOMER authorities.
     * - Spring Security evaluates the /api/admin/** path rule (hasRole('ADMIN')) and rejects the request.
     *
     * Assertion:
     * - Even though the caller presents a cryptographically valid, unexpired token minted with role "ADMIN",
     *   the database role (CUSTOMER) is strictly enforced and the request is immediately blocked
     *   with HTTP 403 Forbidden and JSON code ACCESS_DENIED.
     */
    @Test
    @DisplayName("Scenario 6: JWT stateless role drift test - DB role enforced, downgraded user blocked immediately with 403")
    void jwtStatelessRoleDrift_dbRoleEnforced_returns403Forbidden() throws Exception {
        String email = "demoted_admin@example.com";
        UUID userId = UUID.randomUUID();

        // Mint token with ADMIN claim (simulating token issued prior to role change)
        Authentication originalAdminAuth = new Authentication(userId, email, "hash", "ADMIN", true, true);
        String tokenMintedWithAdminClaim = jwtTokenProvider.generateAccessToken(originalAdminAuth);

        // Database reflects user has been demoted to CUSTOMER
        Authentication currentDbUser = new Authentication(userId, email, "hash", "CUSTOMER", true, true);
        when(userAccountPort.findByEmail(email)).thenReturn(Optional.of(currentDbUser));
        when(authorizationPort.findSubjectByEmail(email))
                .thenReturn(new AuthorizationSubject(userId, email, Set.of("CUSTOMER"), Set.of("PRODUCT_READ", "ORDER_READ")));

        // Calling /api/admin/dashboard must be rejected with 403 Forbidden because DB state governs authorization
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + tokenMintedWithAdminClaim))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value(notNullValue()));
    }
}
