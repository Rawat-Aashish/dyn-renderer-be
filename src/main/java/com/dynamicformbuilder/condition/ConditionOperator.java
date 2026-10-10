package com.dynamicformbuilder.condition;

public enum ConditionOperator {
	EQUALS, NOT_EQUALS, CONTAINS,
	GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL,
	IS_EMPTY, IS_NOT_EMPTY;

	public boolean needsValue() {
		return this != IS_EMPTY && this != IS_NOT_EMPTY;
	}
}
