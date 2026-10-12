package com.dynamicformbuilder.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.dynamicformbuilder.security.JwtService;
import com.dynamicformbuilder.security.SecurityErrorHandlers;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	// Deny by default: every route needs a valid token unless it is listed here.
	// Admin-only routes are then restricted further with @AdminOnly on the controller.
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandlers errorHandlers) {
		http
				.cors(Customizer.withDefaults())
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/forms/*/published").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/forms/*/submissions/start").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/forms/*/submissions").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/forms/*/submissions/files").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/forms/*/submissions/rating").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(errorHandlers.unauthorized())
						.accessDeniedHandler(errorHandlers.forbidden()))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(errorHandlers.unauthorized())
						.accessDeniedHandler(errorHandlers.forbidden()));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// Used by the .cors(...) call above. An empty origin list means no browser origin is allowed.
	@Bean
	CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
		var cors = new CorsConfiguration();
		cors.setAllowedOrigins(appProperties.cors().allowedOrigins());
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("*"));
		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", cors);
		return source;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	JwtEncoder jwtEncoder(AppProperties appProperties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey(appProperties)));
	}

	@Bean
	JwtDecoder jwtDecoder(AppProperties appProperties) {
		return NimbusJwtDecoder.withSecretKey(signingKey(appProperties)).macAlgorithm(MacAlgorithm.HS256).build();
	}

	// Turns the token's "role" claim (e.g. "ADMIN") into the ROLE_ADMIN authority that hasRole('ADMIN') checks.
	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		var authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(JwtService.ROLE_CLAIM);
		authorities.setAuthorityPrefix("ROLE_");
		var converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	private SecretKey signingKey(AppProperties appProperties) {
		return new SecretKeySpec(appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}
}
