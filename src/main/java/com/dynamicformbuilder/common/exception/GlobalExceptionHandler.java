package com.dynamicformbuilder.common.exception;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.dynamicformbuilder.common.response.ApiResponse;

import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.validationError(fieldErrors));
	}

	@ExceptionHandler(FieldErrorsException.class)
	public ResponseEntity<ApiResponse<Void>> handleFieldErrors(FieldErrorsException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.fieldErrors(ex.getMessage(), ex.getErrors()));
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
	}

	@ExceptionHandler(GoneException.class)
	public ResponseEntity<ApiResponse<Void>> handleGone(GoneException ex) {
		return ResponseEntity.status(HttpStatus.GONE).body(ApiResponse.error(ex.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
		String message = "Malformed or missing request body";
		if (ex.getCause() instanceof InvalidFormatException invalid && !invalid.getPath().isEmpty()) {
			String field = invalid.getPath().getLast().getPropertyName();
			message = "Invalid value '" + invalid.getValue() + "' for '" + field + "'";
			if (invalid.getTargetType() != null && invalid.getTargetType().isEnum()) {
				message += ". Allowed values: " + Arrays.toString(invalid.getTargetType().getEnumConstants());
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("Invalid value for '" + ex.getName() + "'"));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.error(ex.getMessage()));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleUnknownRoute(NoResourceFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Endpoint not found"));
	}

	// @AdminOnly runs inside the controller call, so its denial lands here instead of in the security filter
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiResponse.error("You do not have permission to access this resource"));
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid credentials"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Something went wrong"));
	}
}
