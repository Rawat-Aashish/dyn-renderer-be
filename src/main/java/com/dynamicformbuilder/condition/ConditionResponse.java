package com.dynamicformbuilder.condition;

import java.util.List;

public record ConditionResponse(
		Long id,
		String targetFieldCode,
		ConditionAction action,
		LogicalOperator logicalOperator,
		List<Rule> rules) {

	public record Rule(Long id, String sourceFieldCode, ConditionOperator operator, String value) {
	}

	public static ConditionResponse from(FormCondition condition) {
		List<Rule> rules = condition.getRules().stream()
				.map(rule -> new Rule(rule.getId(), rule.getSourceFieldCode(), rule.getOperator(), rule.getValue()))
				.toList();
		return new ConditionResponse(condition.getId(), condition.getTargetFieldCode(), condition.getAction(),
				condition.getLogicalOperator(), rules);
	}
}
