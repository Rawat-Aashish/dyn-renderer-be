package com.dynamicformbuilder.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.dynamicformbuilder.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

// Security filters run before controllers, so GlobalExceptionHandler never sees these
// two failures. They are written here in the same {success, message, data} format.
@Component
public class SecurityErrorHandlers {

	private final ObjectMapper objectMapper;

	public SecurityErrorHandlers(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public AuthenticationEntryPoint unauthorized() {
		return (request, response, ex) -> write(response, HttpStatus.UNAUTHORIZED, "Authentication required");
	}

	public AccessDeniedHandler forbidden() {
		return (request, response, ex) -> write(response, HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
	}

	private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), ApiResponse.error(message));
	}
}
