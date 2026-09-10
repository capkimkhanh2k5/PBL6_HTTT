package com.danasea.backend.modules.admin.presentation;

import java.security.Principal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.Role;
import com.danasea.backend.modules.account.domain.models.User;
import com.danasea.backend.modules.admin.application.usecase.GetUserDetailUseCase;
import com.danasea.backend.modules.admin.application.usecase.GetUsersUseCase;
import com.danasea.backend.modules.admin.application.usecase.LockUserUseCase;
import com.danasea.backend.modules.admin.application.usecase.UnlockUserUseCase;
import com.danasea.backend.modules.admin.presentation.dto.LockUserRequest;
import com.danasea.backend.modules.admin.presentation.dto.UserDetailResponse;
import com.danasea.backend.modules.admin.presentation.dto.UserSummaryResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final GetUsersUseCase getUsersUseCase;
    private final GetUserDetailUseCase getUserDetailUseCase;
    private final LockUserUseCase lockUserUseCase;
    private final UnlockUserUseCase unlockUserUseCase;
    private final AccountInternalApi accountInternalApi;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserSummaryResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean isLocked,
            @RequestParam(required = false) String search,
            Principal principal) {
        checkAdminAccess(principal);
        Pageable pageable = PageRequest.of(page, size);
        Page<UserSummaryResponse> response = getUsersUseCase.execute(pageable, role, isLocked, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDetailResponse> getUserDetail(
            @PathVariable UUID id,
            Principal principal) {
        checkAdminAccess(principal);
        UserDetailResponse response = getUserDetailUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> lockUser(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) LockUserRequest request,
            Principal principal) {
        checkAdminAccess(principal);
        UUID actorId = resolveActorId(principal);
        String reason = request != null ? request.reason() : null;
        lockUserUseCase.execute(actorId, id, reason);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unlockUser(
            @PathVariable UUID id,
            Principal principal) {
        checkAdminAccess(principal);
        UUID actorId = resolveActorId(principal);
        unlockUserUseCase.execute(actorId, id);
        return ResponseEntity.ok().build();
    }

    private void checkAdminAccess(Principal principal) {
        Authentication auth = null;
        if (principal instanceof Authentication a) {
            auth = a;
        } else {
            auth = SecurityContextHolder.getContext().getAuthentication();
        }

        if (auth == null || auth.getAuthorities() == null || auth.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private UUID resolveActorId(Principal principal) {
        if (principal != null && principal.getName() != null) {
            String name = principal.getName();
            try {
                return UUID.fromString(name);
            } catch (IllegalArgumentException ignored) {
                return accountInternalApi.findUserByEmail(name)
                        .map(User::getId)
                        .orElse(null);
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) {
                return accountInternalApi.findUserByEmail(auth.getName())
                        .map(User::getId)
                        .orElse(null);
            }
        }

        return null;
    }
}
