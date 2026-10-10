package com.dynamicformbuilder.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.dynamicformbuilder.security.JwtService;
import com.dynamicformbuilder.user.User;
import com.dynamicformbuilder.user.UserRepository;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final JwtService jwtService;

	public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
			JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.jwtService = jwtService;
	}

	public LoginResponse login(LoginRequest request) {
		authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));

		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

		String token = jwtService.generateToken(user.getEmail(), user.getRole());
		return new LoginResponse(token, jwtService.expiresInSeconds(), user.getFirstName(), user.getLastName(),
				user.getEmail(), user.getRole());
	}
}
