package com.dynamicformbuilder.auth;

import com.dynamicformbuilder.security.Role;

public record LoginResponse(
		String token,
		long expiresIn,
		String firstName,
		String lastName,
		String email,
		Role role) {
}
