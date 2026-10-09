package com.dynamicformbuilder.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@Valid Jwt jwt, @NotNull Cors cors, @NotNull Seed seed) {

	public record Jwt(
			@NotBlank @Size(min = 32, message = "must be at least 32 characters for HS256") String secret,
			@Min(1) long expirationMinutes) {
	}

	// app.cors.allowed-origins=http://localhost:3000,https://admin.example.com
	public record Cors(List<String> allowedOrigins) {
	}

	public record Seed(boolean enabled) {
	}
}
