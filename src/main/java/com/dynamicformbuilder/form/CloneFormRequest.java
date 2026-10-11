package com.dynamicformbuilder.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CloneFormRequest(
		@NotBlank(message = "Form name is required")
		@Size(max = 255, message = "Form name must be at most 255 characters")
		String name,

		@NotBlank(message = "Form code is required")
		@Pattern(regexp = "^[a-z][a-z0-9-]{2,49}$",
				message = "Form code must be 3 to 50 characters: lowercase letters, digits and hyphens, starting with a letter")
		String code) {
}
