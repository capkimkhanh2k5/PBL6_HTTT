package com.danasport.backend.authorization.infrastructure.persistence;

import java.util.Set;
import java.util.UUID;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.danasport.backend.authorization.application.port.AuthorizationPort;
import com.danasport.backend.authorization.domain.model.AuthorizationSubject;
import com.danasport.backend.user.infrastructure.persistence.JpaUserRepository;
import com.danasport.backend.user.infrastructure.persistence.UserEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthorizationAdapter implements AuthorizationPort {

	private final JpaUserRepository userRepository;

	@Override
	public AuthorizationSubject findSubjectByEmail(String email) {
		return userRepository.findByEmail(email)
				.map(this::toSubject)
				.orElseThrow();
	}

	@Override
	public AuthorizationSubject findSubjectByUserId(UUID userId) {
		return userRepository.findById(userId)
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

	private AuthorizationSubject toSubject(UserEntity user) {
		String role = user.getRole().toUpperCase(Locale.ROOT);

		Set<String> permissions = switch (role) {
			case "ADMIN" -> Set.of(
					"USER_READ", "USER_UPDATE",
					"PRODUCT_CREATE", "PRODUCT_READ", "PRODUCT_UPDATE", "PRODUCT_DELETE",
					"ORDER_READ", "ORDER_UPDATE", "ORDER_CANCEL",
					"PROFILE_READ", "PROFILE_UPDATE"
			);
			case "VENDOR" -> Set.of(
					"PRODUCT_CREATE", "PRODUCT_READ", "PRODUCT_UPDATE",
					"ORDER_READ", "ORDER_UPDATE"
			);
			case "USER" -> Set.of(
					"PRODUCT_READ", "ORDER_READ",
					"PROFILE_READ", "PROFILE_UPDATE"
			);
			default -> Set.of();
		};

		return new AuthorizationSubject(
				user.getId(),
				user.getEmail(),
				Set.of(role),
				permissions
		);
	}
}
