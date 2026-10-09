package com.dynamicformbuilder.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtService jwtService;

	@Autowired
	JwtEncoder jwtEncoder;

	@Test
	void requestWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/test/any-user"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void garbageTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/test/any-user").header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminTokenCanReachAdminOnlyRoute() throws Exception {
		String token = jwtService.generateToken("admin", Role.ADMIN);
		mockMvc.perform(get("/api/test/admin").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void tokenWithOtherRoleIsForbiddenOnAdminOnlyRoute() throws Exception {
		String token = tokenWithRole("SOMEONE_ELSE");
		mockMvc.perform(get("/api/test/admin").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void tokenWithOtherRoleCanReachRouteWithoutRoleRestriction() throws Exception {
		String token = tokenWithRole("SOMEONE_ELSE");
		mockMvc.perform(get("/api/test/any-user").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	private String tokenWithRole(String role) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject("someone")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.claim(JwtService.ROLE_CLAIM, role)
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}
}
