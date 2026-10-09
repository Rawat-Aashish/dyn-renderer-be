package com.dynamicformbuilder.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.dynamicformbuilder.config.AppProperties;

@Service
public class JwtService {

	public static final String ROLE_CLAIM = "role";

	private final JwtEncoder jwtEncoder;
	private final Duration expiration;

	public JwtService(JwtEncoder jwtEncoder, AppProperties appProperties) {
		this.jwtEncoder = jwtEncoder;
		this.expiration = Duration.ofMinutes(appProperties.jwt().expirationMinutes());
	}

	public String generateToken(String username, Role role) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(username)
				.issuedAt(now)
				.expiresAt(now.plus(expiration))
				.claim(ROLE_CLAIM, role.name())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public long expiresInSeconds() {
		return expiration.toSeconds();
	}
}
