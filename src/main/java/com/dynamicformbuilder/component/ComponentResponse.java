package com.dynamicformbuilder.component;

import java.util.List;

import com.dynamicformbuilder.validation.ValidationRuleResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.JsonNode;

public record ComponentResponse(
		Long id,
		String fieldCode,
		String fieldLabel,
		ComponentType componentType,
		String placeholder,
		String helpText,
		JsonNode defaultValue,
		Integer displayOrder,
		@JsonProperty("isRequired") boolean required,
		@JsonProperty("isReadOnly") boolean readOnly,
		@JsonProperty("isDisabled") boolean disabled,
		@JsonProperty("isVisible") boolean visible,
		Long parentSectionId,
		ComponentProperties properties,
		List<ValidationRuleResponse> validationRules,
		List<ComponentResponse> children) {

	public static ComponentResponse from(FormComponent component, List<ValidationRuleResponse> validationRules,
			List<ComponentResponse> children) {
		return new ComponentResponse(component.getId(), component.getFieldCode(), component.getFieldLabel(),
				component.getComponentType(), component.getPlaceholder(), component.getHelpText(),
				component.getDefaultValue(), component.getDisplayOrder(), component.isRequired(),
				component.isReadOnly(), component.isDisabled(), component.isVisible(),
				component.getParentSectionId(), component.getProperties(), validationRules, children);
	}
}
