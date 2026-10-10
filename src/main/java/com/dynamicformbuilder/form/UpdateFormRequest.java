package com.dynamicformbuilder.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFormRequest(
		@NotBlank(message = "Form name is required")
		@Size(max = 255, message = "Form name must be at most 255 characters")
		String name,

		String code,

		String description,

		@Size(max = 100, message = "Category must be at most 100 characters")
		String category) {
}
