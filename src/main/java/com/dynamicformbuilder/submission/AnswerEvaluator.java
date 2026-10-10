package com.dynamicformbuilder.submission;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.dynamicformbuilder.component.AnswerFormat;
import com.dynamicformbuilder.component.AnswerRules;
import com.dynamicformbuilder.component.ComponentResponse;
import com.dynamicformbuilder.component.ComponentType;
import com.dynamicformbuilder.condition.ConditionAction;
import com.dynamicformbuilder.condition.ConditionAction.Property;
import com.dynamicformbuilder.condition.ConditionOperator;
import com.dynamicformbuilder.condition.ConditionResponse;
import com.dynamicformbuilder.condition.LogicalOperator;
import com.dynamicformbuilder.formversion.FormConfiguration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Validates one set of submitted answers against one published snapshot (SPEC.md, "Server-side validation
 * on submit"). Create one per submission, call {@link #evaluate()}, then read the errors or the cleaned data.
 *
 * Conditions decide whether a field is visible, enabled and required. Because a hidden field counts as
 * empty for the conditions that read it, visibility is worked out on demand and remembered; the snapshot
 * cannot contain circular conditions, so this always ends.
 */
public class AnswerEvaluator {

	private final JsonNode answers;
	private final Map<String, ComponentResponse> fields = new LinkedHashMap<>();
	private final Map<String, String> sectionOf = new HashMap<>();
	private final Map<String, Map<Property, ConditionResponse>> conditions = new HashMap<>();
	private final Map<String, Boolean> visibility = new HashMap<>();

	private final Map<String, String> errors = new LinkedHashMap<>();
	private final ObjectNode cleaned = JsonNodeFactory.instance.objectNode();

	public AnswerEvaluator(FormConfiguration configuration, JsonNode answers) {
		this.answers = answers;
		for (ComponentResponse top : configuration.components()) {
			fields.put(top.fieldCode(), top);
			for (ComponentResponse child : top.children()) {
				fields.put(child.fieldCode(), child);
				sectionOf.put(child.fieldCode(), top.fieldCode());
			}
		}
		for (ConditionResponse condition : configuration.conditions()) {
			conditions.computeIfAbsent(condition.targetFieldCode(), key -> new EnumMap<>(Property.class))
					.put(condition.action().property(), condition);
		}
	}

	public void evaluate() {
		for (ComponentResponse field : fields.values()) {
			String code = field.fieldCode();
			if (field.componentType().isLayout() || !isVisible(code) || isDisabled(field)) {
				continue;
			}
			if (field.readOnly()) {
				if (!AnswerFormat.isEmpty(field.defaultValue())) {
					cleaned.set(code, field.defaultValue());
				}
				continue;
			}

			JsonNode value = answers.get(code);
			boolean required = isRequired(field);
			if (AnswerFormat.isEmpty(value)) {
				if (required) {
					errors.put(code, "This field is required");
				}
				continue;
			}
			String problem = AnswerRules.check(field, value);
			if (problem == null && required && field.componentType() == ComponentType.TOGGLE && !value.asBoolean()) {
				problem = "This field is required";
			}
			if (problem != null) {
				errors.put(code, problem);
			} else {
				cleaned.set(code, value);
			}
		}
	}

	public Map<String, String> errors() {
		return errors;
	}

	public ObjectNode cleanedData() {
		return cleaned;
	}

	private boolean isVisible(String code) {
		Boolean known = visibility.get(code);
		if (known != null) {
			return known;
		}
		ComponentResponse field = fields.get(code);
		String section = sectionOf.get(code);
		boolean visible = (section == null || isVisible(section))
				&& decide(field, Property.VISIBILITY, ConditionAction.SHOW, field.visible());
		visibility.put(code, visible);
		return visible;
	}

	private boolean isDisabled(ComponentResponse field) {
		return !decide(field, Property.ENABLED, ConditionAction.ENABLE, !field.disabled());
	}

	private boolean isRequired(ComponentResponse field) {
		return decide(field, Property.REQUIRED, ConditionAction.MAKE_REQUIRED, field.required());
	}

	/**
	 * A condition fully controls its property: the "positive" action (SHOW, ENABLE, MAKE_REQUIRED) turns it
	 * on when the rules match and off when they do not, and the opposite action does the reverse. Without a
	 * condition the component's own static flag applies.
	 */
	private boolean decide(ComponentResponse field, Property property, ConditionAction positive, boolean fallback) {
		ConditionResponse condition = conditions.getOrDefault(field.fieldCode(), Map.of()).get(property);
		if (condition == null) {
			return fallback;
		}
		return matches(condition) == (condition.action() == positive);
	}

	private boolean matches(ConditionResponse condition) {
		if (condition.logicalOperator() == LogicalOperator.AND) {
			return condition.rules().stream().allMatch(this::matches);
		}
		return condition.rules().stream().anyMatch(this::matches);
	}

	private boolean matches(ConditionResponse.Rule rule) {
		ComponentResponse source = fields.get(rule.sourceFieldCode());
		JsonNode answer = answerForConditions(source);
		boolean empty = AnswerFormat.isEmpty(answer);
		if (rule.operator() == ConditionOperator.IS_EMPTY) {
			return empty;
		}
		if (rule.operator() == ConditionOperator.IS_NOT_EMPTY) {
			return !empty;
		}
		if (empty) {
			return false;
		}
		try {
			return compare(source.componentType(), answer, rule.operator(), rule.value());
		} catch (NumberFormatException | DateTimeParseException ex) {
			return false;
		}
	}

	private JsonNode answerForConditions(ComponentResponse source) {
		if (!isVisible(source.fieldCode())) {
			return null;
		}
		return source.readOnly() ? source.defaultValue() : answers.get(source.fieldCode());
	}

	private boolean compare(ComponentType type, JsonNode answer, ConditionOperator operator, String expected) {
		return switch (type) {
			case NUMBER, DECIMAL -> answer.isNumber()
					&& ordered(answer.decimalValue().compareTo(new BigDecimal(expected)), operator);
			case DATE -> answer.isString() && ordered(LocalDate.parse(answer.asString(), AnswerFormat.DATE)
					.compareTo(LocalDate.parse(expected, AnswerFormat.DATE)), operator);
			case DATETIME -> answer.isString()
					&& ordered(LocalDateTime.parse(answer.asString(), AnswerFormat.DATE_TIME)
							.compareTo(LocalDateTime.parse(expected, AnswerFormat.DATE_TIME)), operator);
			case TOGGLE -> answer.isBoolean()
					&& ordered(Boolean.compare(answer.asBoolean(), Boolean.parseBoolean(expected)), operator);
			case CHECKBOX, MULTISELECT -> answer.isArray() && containsOption(answer, expected);
			default -> answer.isString() && text(answer.asString(), operator, expected);
		};
	}

	private boolean ordered(int comparison, ConditionOperator operator) {
		return switch (operator) {
			case EQUALS -> comparison == 0;
			case NOT_EQUALS -> comparison != 0;
			case GREATER_THAN -> comparison > 0;
			case LESS_THAN -> comparison < 0;
			case GREATER_THAN_OR_EQUAL -> comparison >= 0;
			case LESS_THAN_OR_EQUAL -> comparison <= 0;
			default -> false;
		};
	}

	private boolean text(String answer, ConditionOperator operator, String expected) {
		return switch (operator) {
			case EQUALS -> answer.equalsIgnoreCase(expected);
			case NOT_EQUALS -> !answer.equalsIgnoreCase(expected);
			case CONTAINS -> answer.toLowerCase().contains(expected.toLowerCase());
			default -> false;
		};
	}

	private boolean containsOption(JsonNode list, String expected) {
		for (JsonNode item : list) {
			if (item.isString() && item.asString().equals(expected)) {
				return true;
			}
		}
		return false;
	}
}
