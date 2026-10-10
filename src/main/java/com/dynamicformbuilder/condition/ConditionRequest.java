package com.dynamicformbuilder.condition;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConditionRequest(
		@NotBlank(message = "Target field code is required")
		String targetFieldCode,

		@NotNull(message = "Action is required")
		ConditionAction action,

		@NotNull(message = "Logical operator is required")
		LogicalOperator logicalOperator,

		@NotEmpty(message = "At least one rule is required")
		@Valid
		List<Rule> rules) {

	public record Rule(
			@NotBlank(message = "Source field code is required")
			String sourceFieldCode,

			@NotNull(message = "Operator is required")
			ConditionOperator operator,

			@Size(max = 500, message = "Value must be at most 500 characters")
			String value) {
	}
}
