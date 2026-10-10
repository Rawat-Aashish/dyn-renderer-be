package com.dynamicformbuilder.formversion;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.dynamicformbuilder.component.AnswerFormat;
import com.dynamicformbuilder.component.AnswerRules;
import com.dynamicformbuilder.component.ComponentProperties.Option;
import com.dynamicformbuilder.component.ComponentResponse;
import com.dynamicformbuilder.component.ComponentType;
import com.dynamicformbuilder.validation.ValidationRuleResponse;
import com.dynamicformbuilder.validation.ValidationRuleType;

/**
 * The checks that only make sense on the whole form, run just before publishing. Broken references,
 * conflicting conditions and cycles are already refused when they are saved, so they are not repeated here.
 */
@Component
public class PublishChecker {

	public static final String FORM_KEY = "form";

	/** Returns problems keyed by field code ("form" for problems with the form as a whole). Empty means publishable. */
	public Map<String, String> check(List<ComponentResponse> tree) {
		Map<String, String> problems = new LinkedHashMap<>();
		boolean hasInput = false;

		for (ComponentResponse top : tree) {
			checkComponent(top, problems);
			hasInput |= !top.componentType().isLayout();
			for (ComponentResponse child : top.children()) {
				checkComponent(child, problems);
				hasInput |= !child.componentType().isLayout();
			}
		}
		if (!hasInput) {
			problems.put(FORM_KEY, "The form needs at least one input field");
		}
		return problems;
	}

	private void checkComponent(ComponentResponse component, Map<String, String> problems) {
		ComponentType type = component.componentType();
		if (isBlank(component.fieldLabel())) {
			add(problems, component, "Field label is missing");
		}
		if (type == ComponentType.SECTION && component.children().isEmpty()) {
			add(problems, component, "Section has no components");
		}
		if (type.hasOptions()) {
			checkOptions(component, problems);
		}

		boolean rulesConsistent = checkRulePairs(component, problems);
		boolean optionsUsable = !type.hasOptions() || !component.properties().options().isEmpty();
		if (rulesConsistent && optionsUsable && !AnswerFormat.isEmpty(component.defaultValue())) {
			String problem = AnswerRules.check(component, component.defaultValue());
			if (problem != null) {
				add(problems, component, "Default value is not valid: " + problem);
			}
		}
	}

	private void checkOptions(ComponentResponse component, Map<String, String> problems) {
		List<Option> options = component.properties().options();
		if (options.isEmpty()) {
			add(problems, component, "Add at least one option");
			return;
		}
		Set<String> seen = new HashSet<>();
		Set<String> repeated = options.stream().map(Option::value).filter(value -> !seen.add(value))
				.collect(Collectors.toCollection(java.util.LinkedHashSet::new));
		if (!repeated.isEmpty()) {
			add(problems, component, "Option values must be unique (repeated: " + String.join(", ", repeated) + ")");
		}
	}

	private boolean checkRulePairs(ComponentResponse component, Map<String, String> problems) {
		Map<ValidationRuleType, String> values = component.validationRules().stream()
				.collect(Collectors.toMap(ValidationRuleResponse::ruleType, ValidationRuleResponse::ruleValue));
		boolean consistent = true;

		String minLength = values.get(ValidationRuleType.MIN_LENGTH);
		String maxLength = values.get(ValidationRuleType.MAX_LENGTH);
		if (minLength != null && maxLength != null && Integer.parseInt(minLength) > Integer.parseInt(maxLength)) {
			add(problems, component, "MIN_LENGTH (" + minLength + ") is greater than MAX_LENGTH (" + maxLength + ")");
			consistent = false;
		}
		String min = values.get(ValidationRuleType.MIN);
		String max = values.get(ValidationRuleType.MAX);
		if (min != null && max != null && new BigDecimal(min).compareTo(new BigDecimal(max)) > 0) {
			add(problems, component, "MIN (" + min + ") is greater than MAX (" + max + ")");
			consistent = false;
		}
		// Both date formats sort correctly as plain text (yyyy-MM-dd and yyyy-MM-ddTHH:mm)
		String dateMin = values.get(ValidationRuleType.DATE_MIN);
		String dateMax = values.get(ValidationRuleType.DATE_MAX);
		if (dateMin != null && dateMax != null && dateMin.compareTo(dateMax) > 0) {
			add(problems, component, "DATE_MIN (" + dateMin + ") is after DATE_MAX (" + dateMax + ")");
			consistent = false;
		}
		return consistent;
	}

	private void add(Map<String, String> problems, ComponentResponse component, String message) {
		problems.merge(component.fieldCode(), message, (existing, added) -> existing + "; " + added);
	}

	private boolean isBlank(String text) {
		return text == null || text.isBlank();
	}
}
