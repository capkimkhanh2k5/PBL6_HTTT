package com.danasea.backend.modules.admin.integration;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.account.application.service.AccountInternalService;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.audit.infrastructure.mapper.AuditLogMapper;
import com.danasea.backend.modules.audit.infrastructure.adapter.AuditLogAdapter;
import com.danasea.backend.modules.account.infrastructure.mapper.RefreshTokenMapper;
import com.danasea.backend.modules.account.infrastructure.mapper.UserMapper;
import com.danasea.backend.modules.audit.infrastructure.persistence.entities.AuditLogJpaEntity;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.RefreshTokenJpaEntity;
import com.danasea.backend.modules.account.infrastructure.persistence.entities.UserJpaEntity;
import com.danasea.backend.modules.audit.infrastructure.persistence.repositories.JpaAuditLogRepository;
import com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaRefreshTokenRepository;
import com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaUserRepository;
import com.danasea.backend.modules.admin.application.usecase.LockUserUseCase;
import com.danasea.backend.security.authentication.application.port.TokenProvider;
import com.danasea.backend.security.authentication.application.usecase.RefreshTokenUseCase;
import com.danasea.backend.security.authentication.domain.exception.InvalidCredentialsException;
import com.danasea.backend.security.authentication.infrastructure.security.HashUtils;
import com.danasea.backend.security.authentication.infrastructure.security.JwtProperties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLockTokenRevocationIntegrationTest {

    @Mock
    private JpaUserRepository userRepository;

    @Mock
    private JpaRefreshTokenRepository refreshTokenRepository;

    @Mock
    private JpaAuditLogRepository auditLogRepository;

    @Mock
    private TokenProvider tokenProvider;

    private AccountInternalService accountInternalService;
    private AuditLogAdapter auditLogAdapter;
    private LockUserUseCase lockUserUseCase;
    private RefreshTokenUseCase refreshTokenUseCase;

    private final List<UserJpaEntity> userStore = new ArrayList<>();
    private final List<RefreshTokenJpaEntity> tokenStore = new ArrayList<>();
    private final List<AuditLogJpaEntity> auditStore = new ArrayList<>();

    private UUID adminId;
    private UUID targetUserId;
    private String rawRefreshToken1;
    private String rawRefreshToken2;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = new UserMapper();
        RefreshTokenMapper refreshTokenMapper = new RefreshTokenMapper();
        AuditLogMapper auditLogMapper = new AuditLogMapper();

        accountInternalService = new AccountInternalService(
                userRepository,
                refreshTokenRepository,
                userMapper,
                refreshTokenMapper,
                mock(org.springframework.cache.CacheManager.class)
        );
        
        auditLogAdapter = new AuditLogAdapter(auditLogRepository, auditLogMapper);

        lockUserUseCase = new LockUserUseCase(accountInternalService, auditLogAdapter);

        JwtProperties jwtProperties = new JwtProperties("super-secret-key-that-is-long-enough-32bytes", 15, 7);
        refreshTokenUseCase = new RefreshTokenUseCase(accountInternalService, tokenProvider, jwtProperties);

        adminId = UUID.randomUUID();
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

        when(userRepository.findById(targetUserId)).thenAnswer(inv ->
                userStore.stream().filter(u -> u.getId().equals(targetUserId)).findFirst()
        );
        when(userRepository.saveAndFlush(any(UserJpaEntity.class))).thenAnswer(inv -> {
            UserJpaEntity u = inv.getArgument(0);
            userStore.removeIf(existing -> existing.getId().equals(u.getId()));
            userStore.add(u);
            return u;
        });

        when(refreshTokenRepository.findAllByUserId(targetUserId)).thenAnswer(inv ->
                tokenStore.stream().filter(t -> t.getUserId().equals(targetUserId)).toList()
        );
        
        // Mock findByTokenHash for both tokens
        when(refreshTokenRepository.findByTokenHash(anyString())).thenAnswer(inv -> {
            String hash = inv.getArgument(0);
            return tokenStore.stream().filter(t -> t.getTokenHash().equals(hash)).findFirst();
        });
        
        when(refreshTokenRepository.saveAll(any())).thenAnswer(inv -> {
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

        when(auditLogRepository.save(any(AuditLogJpaEntity.class))).thenAnswer(inv -> {
            AuditLogJpaEntity log = inv.getArgument(0);
            auditStore.add(log);
            return log;
        });
    }

    @Test
    void shouldRevokeAllTokenFamiliesAndRejectRefreshWhenUserIsLocked() {
        User beforeLockUser = accountInternalService.findUserById(targetUserId).orElseThrow();
        assertFalse(beforeLockUser.getIsLocked());

        lockUserUseCase.execute(adminId, targetUserId, "Suspicious activity detected");

        User afterLockUser = accountInternalService.findUserById(targetUserId).orElseThrow();
        assertTrue(afterLockUser.getIsLocked());

        assertEquals(1, auditStore.size());
        assertEquals("USER_LOCKED", auditStore.get(0).getAction());
        assertEquals(targetUserId, auditStore.get(0).getEntityId());
        assertEquals(adminId, auditStore.get(0).getActorUserId());

        // Verify ALL tokens across ALL families are revoked
        assertEquals(2, tokenStore.size());
        for (RefreshTokenJpaEntity token : tokenStore) {
            assertNotNull(token.getRevokedAt(), "Token must be marked as revoked across all families");
        }

        // Try to refresh with Token 1
        assertThrows(
                InvalidCredentialsException.class,
                () -> refreshTokenUseCase.execute(rawRefreshToken1),
                "Should reject refresh with token from Family 1"
        );
        
        // Try to refresh with Token 2
        assertThrows(
                InvalidCredentialsException.class,
                () -> refreshTokenUseCase.execute(rawRefreshToken2),
                "Should reject refresh with token from Family 2"
        );
    }
}
