package com.danasea.backend.security.authorization.presentation;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/authorization")
public class AuthorizationController {

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public String adminAccess() {
		return "ADMIN access granted";
	}

	@GetMapping("/vendor")
	@PreAuthorize("hasRole('VENDOR')")
	public String vendorAccess() {
		return "VENDOR access granted";
	}

	@GetMapping("/user")
	@PreAuthorize("hasRole('USER')")
	public String userAccess() {
		return "USER access granted";
	}

	@GetMapping("/product-read")
	@PreAuthorize("hasAuthority('PRODUCT_READ')")
	public String productReadAccess() {
		return "PRODUCT_READ access granted";
	}
}
