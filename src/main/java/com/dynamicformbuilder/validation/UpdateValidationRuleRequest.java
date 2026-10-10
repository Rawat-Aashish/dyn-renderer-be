package com.dynamicformbuilder.validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateValidationRuleRequest(
		@NotBlank(message = "Rule value is required")
		@Size(max = 500, message = "Rule value must be at most 500 characters")
		String ruleValue,

		@Size(max = 500, message = "Error message must be at most 500 characters")
		String errorMessage) {
}
