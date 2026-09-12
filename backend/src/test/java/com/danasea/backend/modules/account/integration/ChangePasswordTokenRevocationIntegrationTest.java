package com.danasea.backend.modules.account.integration;

import com.danasea.backend.modules.account.application.service.AccountInternalService;
import com.danasea.backend.modules.account.application.usecases.ChangePasswordUseCase;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.account.infrastructure.mappers.RefreshTokenMapper;
import com.danasea.backend.modules.account.infrastructure.mappers.UserMapper;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.RefreshTokenJpaEntity;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.UserJpaEntity;
import com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaRefreshTokenRepository;
import com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaUserRepository;
import com.danasea.backend.modules.audit.application.api.AuditLogInternalApi;
import com.danasea.backend.security.authentication.application.ports.PasswordHasher;
import com.danasea.backend.security.authentication.application.ports.TokenProvider;
import com.danasea.backend.security.authentication.application.usecases.RefreshTokenUseCase;
import com.danasea.backend.security.authentication.domain.exceptions.InvalidCredentialsException;
import com.danasea.backend.security.authentication.infrastructure.security.HashUtils;
import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordTokenRevocationIntegrationTest {

    @Mock
    private JpaUserRepository userRepository;

    @Mock
    private JpaRefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuditLogInternalApi auditLogInternalApi;

    @Mock
    private TokenProvider tokenProvider;
    
    @Mock
    private PasswordHasher passwordHasher;

    private AccountInternalService accountInternalService;
    private ChangePasswordUseCase changePasswordUseCase;
    private RefreshTokenUseCase refreshTokenUseCase;

    private final List<UserJpaEntity> userStore = new ArrayList<>();
    private final List<RefreshTokenJpaEntity> tokenStore = new ArrayList<>();

    private UUID targetUserId;
    private String rawRefreshToken1;
    private String rawRefreshToken2;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = new UserMapper();
        RefreshTokenMapper refreshTokenMapper = new RefreshTokenMapper();

        accountInternalService = new AccountInternalService(
                userRepository,
                refreshTokenRepository,
                userMapper,
                refreshTokenMapper,
                mock(CacheManager.class));

        changePasswordUseCase = new ChangePasswordUseCase(accountInternalService, passwordHasher, auditLogInternalApi);

        JwtProperties jwtProperties = new JwtProperties("super-secret-key-that-is-long-enough-32bytes", 15, 7);
        refreshTokenUseCase = new RefreshTokenUseCase(accountInternalService, tokenProvider, jwtProperties);

        targetUserId = UUID.randomUUID();

        rawRefreshToken1 = "test-raw-refresh-token-family1-" + UUID.randomUUID();
        rawRefreshToken2 = "test-raw-refresh-token-family2-" + UUID.randomUUID();
        String tokenHash1 = HashUtils.sha256(rawRefreshToken1);
        String tokenHash2 = HashUtils.sha256(rawRefreshToken2);

        UserJpaEntity userEntity = new UserJpaEntity();
        userEntity.setId(targetUserId);
        userEntity.setEmail("target@example.com");
        userEntity.setFullName("Target User");
        userEntity.setRole(Role.CUSTOMER);
        userEntity.setPasswordHash("hashed_old_password");
        userEntity.setIsLocked(false);
        userEntity.setIsEmailVerified(true);
        userStore.add(userEntity);

        // Token 1 - Family 1
        RefreshTokenJpaEntity tokenEntity1 = RefreshTokenJpaEntity.builder()
                .id(UUID.randomUUID())
                .userId(targetUserId)
                .tokenHash(tokenHash1)
                .familyId(UUID.randomUUID()) // Family A
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .revokedAt(null)
                .build();
        tokenStore.add(tokenEntity1);

        // Token 2 - Family 2 (Different session)
        RefreshTokenJpaEntity tokenEntity2 = RefreshTokenJpaEntity.builder()
                .id(UUID.randomUUID())
                .userId(targetUserId)
                .tokenHash(tokenHash2)
                .familyId(UUID.randomUUID()) // Family B
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .revokedAt(null)
                .build();
        tokenStore.add(tokenEntity2);

        lenient().when(userRepository.findById(targetUserId))
                .thenAnswer(inv -> userStore.stream().filter(u -> u.getId().equals(targetUserId)).findFirst());
        lenient().when(userRepository.findByEmail("target@example.com"))
                .thenAnswer(inv -> userStore.stream().filter(u -> "target@example.com".equals(u.getEmail())).findFirst());
        lenient().when(userRepository.saveAndFlush(any(UserJpaEntity.class))).thenAnswer(inv -> {
            UserJpaEntity u = inv.getArgument(0);
            userStore.removeIf(existing -> existing.getId().equals(u.getId()));
            userStore.add(u);
            return u;
        });

        lenient().when(refreshTokenRepository.findAllByUserId(targetUserId))
                .thenAnswer(inv -> tokenStore.stream().filter(t -> t.getUserId().equals(targetUserId)).toList());

        // Mock findByTokenHash for both tokens
        lenient().when(refreshTokenRepository.findByTokenHash(anyString())).thenAnswer(inv -> {
            String hash = inv.getArgument(0);
            return tokenStore.stream().filter(t -> t.getTokenHash().equals(hash)).findFirst();
        });

        lenient().when(refreshTokenRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<RefreshTokenJpaEntity> tokens = inv.getArgument(0);
            tokens.forEach(t -> {
                tokenStore.removeIf(existing -> existing.getId().equals(t.getId()));
                tokenStore.add(t);
            });
            return tokenStore;
        });

        lenient().when(refreshTokenRepository.findAllByFamilyId(any(UUID.class))).thenAnswer(inv -> {
            UUID familyId = inv.getArgument(0);
            return tokenStore.stream().filter(t -> familyId.equals(t.getFamilyId())).toList();
        });
    }

    @Test
    void shouldRevokeAllTokenFamiliesAndRejectRefreshAfterPasswordChange() {
        // Assume user correctly provides old password
        when(passwordHasher.matches("oldPassword", "hashed_old_password")).thenReturn(true);
        when(passwordHasher.hash("NewPassword123!")).thenReturn("hashed_new_password");

        // Execute password change
        changePasswordUseCase.execute("target@example.com", "oldPassword", "NewPassword123!");

        // Verify password was changed
        User afterChangeUser = accountInternalService.findUserByEmail("target@example.com").orElseThrow();
        assertEquals("hashed_new_password", afterChangeUser.getPasswordHash());

        // Verify ALL tokens across ALL families are revoked
        assertEquals(2, tokenStore.size());
        for (RefreshTokenJpaEntity token : tokenStore) {
            assertNotNull(token.getRevokedAt(), "Token must be marked as revoked across all families");
        }

        // Try to refresh with Token 1 -> should fail immediately
        assertThrows(
                InvalidCredentialsException.class,
                () -> refreshTokenUseCase.execute(rawRefreshToken1),
                "Should reject refresh with token from Family 1 because it's revoked"
        );

        // Try to refresh with Token 2 -> should fail immediately
        assertThrows(
                InvalidCredentialsException.class,
                () -> refreshTokenUseCase.execute(rawRefreshToken2),
                "Should reject refresh with token from Family 2 because it's revoked"
        );
    }
}
