package com.dynamicformbuilder.common.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
		boolean success,
		String message,
		T data,
		@JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> errors) {

	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(true, message, data, null);
	}

	public static <T> ApiResponse<T> error(String message) {
		return new ApiResponse<>(false, message, null, null);
	}

	public static <T> ApiResponse<T> validationError(Map<String, String> errors) {
		return new ApiResponse<>(false, "Validation failed", null, errors);
	}
}
