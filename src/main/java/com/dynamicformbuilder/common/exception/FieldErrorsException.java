package com.dynamicformbuilder.common.exception;

import java.util.Map;

/** A 400 that carries a message per field, for checks that cannot be expressed as @Valid annotations. */
public class FieldErrorsException extends RuntimeException {

	private final transient Map<String, String> errors;

	public FieldErrorsException(String message, Map<String, String> errors) {
		super(message);
		this.errors = errors;
	}

	public Map<String, String> getErrors() {
		return errors;
	}
}
