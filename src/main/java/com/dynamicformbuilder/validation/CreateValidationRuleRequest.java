package com.dynamicformbuilder.validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateValidationRuleRequest(
		@NotNull(message = "Rule type is required")
		ValidationRuleType ruleType,

		@NotBlank(message = "Rule value is required")
		@Size(max = 500, message = "Rule value must be at most 500 characters")
		String ruleValue,

		@Size(max = 500, message = "Error message must be at most 500 characters")
		String errorMessage) {
}
