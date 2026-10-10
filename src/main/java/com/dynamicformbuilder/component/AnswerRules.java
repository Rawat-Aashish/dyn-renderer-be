package com.dynamicformbuilder.component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.dynamicformbuilder.validation.ValidationRuleResponse;

import tools.jackson.databind.JsonNode;

/**
 * Checks one non-empty answer against everything a component demands: the value shape for its type, its
 * validation rules, and its options. Publishing uses it for default values and submissions use it for
 * answers, so a default that passes here is a value an end user could also submit.
 */
public final class AnswerRules {

	private AnswerRules() {
	}

	/** Returns null when the value is acceptable, otherwise the message to show for the field. */
	public static String check(ComponentResponse component, JsonNode value) {
		ComponentType type = component.componentType();
		String problem = AnswerFormat.check(type, component.properties(), value);
		if (problem != null) {
			return problem;
		}
		for (ValidationRuleResponse rule : component.validationRules()) {
			if (breaks(rule, type, value)) {
				return rule.errorMessage() != null ? rule.errorMessage() : defaultMessage(rule);
			}
		}
		if (type.hasOptions() && !allAreOptions(component, value)) {
			return "Must be one of the available options";
		}
		return null;
	}

	private static boolean breaks(ValidationRuleResponse rule, ComponentType type, JsonNode value) {
		String limit = rule.ruleValue();
		return switch (rule.ruleType()) {
			case MIN_LENGTH -> value.asString().length() < Integer.parseInt(limit);
			case MAX_LENGTH -> value.asString().length() > Integer.parseInt(limit);
			case REGEX -> !Pattern.compile(limit).matcher(value.asString()).matches();
			case MIN -> value.decimalValue().compareTo(new BigDecimal(limit)) < 0;
			case MAX -> value.decimalValue().compareTo(new BigDecimal(limit)) > 0;
			case DATE_MIN -> compareDates(type, value.asString(), limit) < 0;
			case DATE_MAX -> compareDates(type, value.asString(), limit) > 0;
		};
	}

	private static int compareDates(ComponentType type, String answer, String limit) {
		if (type == ComponentType.DATETIME) {
			return LocalDateTime.parse(answer, AnswerFormat.DATE_TIME)
					.compareTo(LocalDateTime.parse(limit, AnswerFormat.DATE_TIME));
		}
		return LocalDate.parse(answer, AnswerFormat.DATE).compareTo(LocalDate.parse(limit, AnswerFormat.DATE));
	}

	private static String defaultMessage(ValidationRuleResponse rule) {
		String limit = rule.ruleValue();
		return switch (rule.ruleType()) {
			case MIN_LENGTH -> "Must be at least " + limit + " characters";
			case MAX_LENGTH -> "Must be at most " + limit + " characters";
			case REGEX -> "Is not in the expected format";
			case MIN -> "Must be at least " + limit;
			case MAX -> "Must be at most " + limit;
			case DATE_MIN -> "Must be on or after " + limit;
			case DATE_MAX -> "Must be on or before " + limit;
		};
	}

	private static boolean allAreOptions(ComponentResponse component, JsonNode value) {
		Set<String> allowed = component.properties().options().stream()
				.map(ComponentProperties.Option::value)
				.collect(Collectors.toSet());
		if (value.isArray()) {
			for (JsonNode item : value) {
				if (!allowed.contains(item.asString())) {
					return false;
				}
			}
			return true;
		}
		return allowed.contains(value.asString());
	}
}
