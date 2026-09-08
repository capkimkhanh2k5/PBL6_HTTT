package com.danasea.backend.security.authorization.infrastructure.persistence;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.security.authorization.application.port.AuthorizationPort;
import com.danasea.backend.security.authorization.domain.model.AuthorizationSubject;
import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthorizationAdapter implements AuthorizationPort {

	private final AccountInternalApi accountApi;

	@Override
	public AuthorizationSubject findSubjectByEmail(String email) {
		return accountApi.findUserByEmail(email)
				.map(this::toSubject)
				.orElseThrow();
	}

	@Override
	public AuthorizationSubject findSubjectByUserId(UUID userId) {
		return accountApi.findUserById(userId)
				.map(this::toSubject)
				.orElseThrow();
	}

	@Override
	public Set<String> findRolesByUserId(UUID userId) {
		return findSubjectByUserId(userId).roles();
	}

	@Override
	public Set<String> findPermissionsByUserId(UUID userId) {
		return findSubjectByUserId(userId).permissions();
	}

	@Override
	public boolean hasPermission(UUID userId, String permissionCode) {
		return findPermissionsByUserId(userId).contains(permissionCode);
	}

	private AuthorizationSubject toSubject(User user) {
		String role = user.getRole() != null ? user.getRole().name() : "CUSTOMER";

		Set<String> permissions = switch (role) {
			case "ADMIN" -> Set.of(
					"USER_READ", "USER_UPDATE",
					"PRODUCT_CREATE", "PRODUCT_READ", "PRODUCT_UPDATE", "PRODUCT_DELETE",
					"ORDER_READ", "ORDER_UPDATE", "ORDER_CANCEL",
					"PROFILE_READ", "PROFILE_UPDATE");
			case "VENDOR" -> Set.of(
					"PRODUCT_CREATE", "PRODUCT_READ", "PRODUCT_UPDATE",
					"ORDER_READ", "ORDER_UPDATE");
			case "CUSTOMER" -> Set.of(
					"PRODUCT_READ", "ORDER_READ",
					"PROFILE_READ", "PROFILE_UPDATE");
			default -> Set.of();
		};

		return new AuthorizationSubject(
				user.getId(),
				user.getEmail(),
				Set.of(role),
				permissions);
	}
}
