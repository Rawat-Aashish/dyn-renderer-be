package com.dynamicformbuilder.validation;

import static com.dynamicformbuilder.component.ComponentType.DATE;
import static com.dynamicformbuilder.component.ComponentType.DATETIME;
import static com.dynamicformbuilder.component.ComponentType.DECIMAL;
import static com.dynamicformbuilder.component.ComponentType.EMAIL;
import static com.dynamicformbuilder.component.ComponentType.NUMBER;
import static com.dynamicformbuilder.component.ComponentType.PHONE;
import static com.dynamicformbuilder.component.ComponentType.TEXT;
import static com.dynamicformbuilder.component.ComponentType.TEXTAREA;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dynamicformbuilder.common.exception.BadRequestException;
import com.dynamicformbuilder.common.exception.ConflictException;
import com.dynamicformbuilder.common.exception.ResourceNotFoundException;
import com.dynamicformbuilder.component.AnswerFormat;
import com.dynamicformbuilder.component.ComponentRepository;
import com.dynamicformbuilder.component.ComponentType;
import com.dynamicformbuilder.component.FormComponent;
import com.dynamicformbuilder.form.FormService;

@Service
public class ValidationRuleService {

	private static final Set<ComponentType> TEXT_LIKE = Set.of(TEXT, TEXTAREA, EMAIL, PHONE);
	private static final Set<ComponentType> NUMERIC = Set.of(NUMBER, DECIMAL);
	private static final Set<ComponentType> DATES = Set.of(DATE, DATETIME);

	private static final Map<ValidationRuleType, Set<ComponentType>> ALLOWED_ON = Map.of(
			ValidationRuleType.MIN_LENGTH, TEXT_LIKE,
			ValidationRuleType.MAX_LENGTH, TEXT_LIKE,
			ValidationRuleType.REGEX, Set.of(TEXT, TEXTAREA, PHONE),
			ValidationRuleType.MIN, NUMERIC,
			ValidationRuleType.MAX, NUMERIC,
			ValidationRuleType.DATE_MIN, DATES,
			ValidationRuleType.DATE_MAX, DATES);

	private final ValidationRuleRepository validationRuleRepository;
	private final ComponentRepository componentRepository;
	private final FormService formService;

	public ValidationRuleService(ValidationRuleRepository validationRuleRepository,
			ComponentRepository componentRepository, FormService formService) {
		this.validationRuleRepository = validationRuleRepository;
		this.componentRepository = componentRepository;
		this.formService = formService;
	}

	@Transactional
	public ValidationRuleResponse create(Long componentId, CreateValidationRuleRequest request) {
		FormComponent component = editableComponent(componentId);
		ComponentType type = component.getComponentType();
		if (!ALLOWED_ON.get(request.ruleType()).contains(type)) {
			throw new BadRequestException(request.ruleType() + " cannot be used on " + type + " components");
		}
		if (validationRuleRepository.existsByComponentIdAndRuleType(componentId, request.ruleType())) {
			throw new ConflictException("This component already has a " + request.ruleType() + " rule");
		}
		requireValidValue(request.ruleType(), type, request.ruleValue());

		ValidationRule rule = new ValidationRule();
		rule.setComponentId(componentId);
		rule.setRuleType(request.ruleType());
		rule.setRuleValue(request.ruleValue().trim());
		rule.setErrorMessage(blankToNull(request.errorMessage()));
		return ValidationRuleResponse.from(validationRuleRepository.save(rule));
	}

	@Transactional
	public ValidationRuleResponse update(Long componentId, Long ruleId, UpdateValidationRuleRequest request) {
		FormComponent component = editableComponent(componentId);
		ValidationRule rule = findRule(componentId, ruleId);
		requireValidValue(rule.getRuleType(), component.getComponentType(), request.ruleValue());

		rule.setRuleValue(request.ruleValue().trim());
		rule.setErrorMessage(blankToNull(request.errorMessage()));
		return ValidationRuleResponse.from(validationRuleRepository.save(rule));
	}

	@Transactional
	public void delete(Long componentId, Long ruleId) {
		editableComponent(componentId);
		validationRuleRepository.delete(findRule(componentId, ruleId));
	}

	private FormComponent editableComponent(Long componentId) {
		FormComponent component = componentRepository.findById(componentId)
				.orElseThrow(() -> new ResourceNotFoundException("Component not found"));
		formService.getEditableForm(component.getFormId());
		return component;
	}

	private ValidationRule findRule(Long componentId, Long ruleId) {
		return validationRuleRepository.findByIdAndComponentId(ruleId, componentId)
				.orElseThrow(() -> new ResourceNotFoundException("Validation rule not found"));
	}

	private void requireValidValue(ValidationRuleType ruleType, ComponentType componentType, String rawValue) {
		String value = rawValue.trim();
		try {
			switch (ruleType) {
				case MIN_LENGTH, MAX_LENGTH -> {
					if (Integer.parseInt(value) < 0) {
						throw new BadRequestException(ruleType + " value must be zero or a positive whole number");
					}
				}
				case REGEX -> Pattern.compile(value);
				case MIN, MAX -> new BigDecimal(value);
				case DATE_MIN, DATE_MAX -> {
					if (componentType == DATETIME) {
						LocalDateTime.parse(value, AnswerFormat.DATE_TIME);
					} else {
						LocalDate.parse(value, AnswerFormat.DATE);
					}
				}
			}
		} catch (NumberFormatException ex) {
			throw new BadRequestException(ruleType + " value must be a number");
		} catch (PatternSyntaxException ex) {
			throw new BadRequestException("REGEX value is not a valid pattern");
		} catch (DateTimeParseException ex) {
			throw new BadRequestException(ruleType + " value must be in the format "
					+ (componentType == DATETIME ? "yyyy-MM-ddTHH:mm" : "yyyy-MM-dd"));
		}
	}

	private String blankToNull(String text) {
		return text == null || text.isBlank() ? null : text.trim();
	}
}
