package com.dynamicformbuilder.component;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record ComponentRequest(
		@NotBlank(message = "Field code is required")
		@Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,99}$",
				message = "Field code must start with a letter and contain only letters, digits and underscores (max 100 characters)")
		String fieldCode,

		@NotBlank(message = "Field label is required")
		@Size(max = 255, message = "Field label must be at most 255 characters")
		String fieldLabel,

		@NotNull(message = "Component type is required")
		ComponentType componentType,

		@Size(max = 255, message = "Placeholder must be at most 255 characters")
		String placeholder,

		@Size(max = 500, message = "Help text must be at most 500 characters")
		String helpText,

		JsonNode defaultValue,

		@JsonProperty("isRequired") Boolean required,
		@JsonProperty("isReadOnly") Boolean readOnly,
		@JsonProperty("isDisabled") Boolean disabled,
		@JsonProperty("isVisible") Boolean visible,

		Long parentSectionId,

		ComponentProperties properties) {
}
