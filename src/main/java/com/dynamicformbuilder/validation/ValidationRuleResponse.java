package com.dynamicformbuilder.validation;

public record ValidationRuleResponse(Long id, ValidationRuleType ruleType, String ruleValue, String errorMessage) {

	public static ValidationRuleResponse from(ValidationRule rule) {
		return new ValidationRuleResponse(rule.getId(), rule.getRuleType(), rule.getRuleValue(),
				rule.getErrorMessage());
	}
}
