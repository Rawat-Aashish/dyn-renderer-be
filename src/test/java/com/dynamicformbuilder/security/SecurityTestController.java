package com.dynamicformbuilder.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurityTestController {

	@GetMapping("/api/test/admin")
	@AdminOnly
	String adminOnly() {
		return "ok";
	}

	@GetMapping("/api/test/any-user")
	String anyAuthenticatedUser() {
		return "ok";
	}
}
