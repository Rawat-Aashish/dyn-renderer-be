package com.dynamicformbuilder.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dynamicformbuilder.user.User;
import com.dynamicformbuilder.user.UserRepository;

@SpringBootTest
class UserAuthenticationTest {

	private static final String EMAIL = "auth-test@example.com";

	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@BeforeEach
	void createUser() {
		var user = new User();
		user.setFirstName("Auth");
		user.setEmail(EMAIL);
		user.setPassword(passwordEncoder.encode("secret-pass"));
		user.setRole(Role.ADMIN);
		userRepository.save(user);
	}

	@AfterEach
	void deleteUser() {
		userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
	}

	@Test
	void correctPasswordAuthenticatesWithAdminRole() {
		var result = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, "secret-pass"));

		assertThat(result.isAuthenticated()).isTrue();
		assertThat(result.getAuthorities()).extracting(Object::toString).contains("ROLE_ADMIN");
	}

	@Test
	void wrongPasswordIsRejected() {
		assertThatThrownBy(() -> authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, "wrong")))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void unknownEmailIsRejected() {
		assertThatThrownBy(() -> authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated("nobody@example.com", "secret-pass")))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void seededAdminExistsWithHashedPassword() {
		var admin = userRepository.findByEmail("admin@gmail.com").orElseThrow();

		assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
		assertThat(admin.getPassword()).isNotEqualTo("password");
		assertThat(passwordEncoder.matches("password", admin.getPassword())).isTrue();
	}
}
