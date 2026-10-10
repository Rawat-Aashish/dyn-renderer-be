package com.dynamicformbuilder.condition;

import static com.dynamicformbuilder.component.ComponentType.CHECKBOX;
import static com.dynamicformbuilder.component.ComponentType.DATE;
import static com.dynamicformbuilder.component.ComponentType.DATETIME;
import static com.dynamicformbuilder.component.ComponentType.DECIMAL;
import static com.dynamicformbuilder.component.ComponentType.EMAIL;
import static com.dynamicformbuilder.component.ComponentType.MULTISELECT;
import static com.dynamicformbuilder.component.ComponentType.NUMBER;
import static com.dynamicformbuilder.component.ComponentType.PHONE;
import static com.dynamicformbuilder.component.ComponentType.TEXT;
import static com.dynamicformbuilder.component.ComponentType.TEXTAREA;
import static com.dynamicformbuilder.component.ComponentType.TOGGLE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
public class ConditionService {

	private static final Set<ComponentType> CONTAINS_TYPES = Set.of(TEXT, TEXTAREA, EMAIL, PHONE, CHECKBOX, MULTISELECT);
	private static final Set<ComponentType> ORDERED_TYPES = Set.of(NUMBER, DECIMAL, DATE, DATETIME);

	private final ConditionRepository conditionRepository;
	private final ComponentRepository componentRepository;
	private final FormService formService;

	public ConditionService(ConditionRepository conditionRepository, ComponentRepository componentRepository,
			FormService formService) {
		this.conditionRepository = conditionRepository;
		this.componentRepository = componentRepository;
		this.formService = formService;
	}

	@Transactional
	public ConditionResponse create(Long formId, ConditionRequest request) {
		formService.getEditableForm(formId);
		FormCondition condition = new FormCondition();
		condition.setFormId(formId);
		return save(condition, request);
	}

	@Transactional
	public ConditionResponse update(Long formId, Long conditionId, ConditionRequest request) {
		formService.getEditableForm(formId);
		return save(findCondition(formId, conditionId), request);
	}

	@Transactional
	public void delete(Long formId, Long conditionId) {
		formService.getEditableForm(formId);
		conditionRepository.delete(findCondition(formId, conditionId));
	}

	private FormCondition findCondition(Long formId, Long conditionId) {
		return conditionRepository.findByIdAndFormId(conditionId, formId)
				.orElseThrow(() -> new ResourceNotFoundException("Condition not found"));
	}

	private ConditionResponse save(FormCondition condition, ConditionRequest request) {
		Long formId = condition.getFormId();
		List<FormComponent> components = componentRepository.findByFormIdOrderByDisplayOrderAscIdAsc(formId);
		Map<String, FormComponent> byCode = components.stream()
				.collect(Collectors.toMap(FormComponent::getFieldCode, Function.identity()));
		List<FormCondition> others = conditionRepository.findByFormIdOrderByIdAsc(formId).stream()
				.filter(other -> !other.getId().equals(condition.getId()))
				.toList();

		FormComponent target = byCode.get(request.targetFieldCode());
		if (target == null) {
			throw new BadRequestException("Target field '" + request.targetFieldCode() + "' does not exist in this form");
		}
		if (target.getComponentType().isLayout()
				&& request.action().property() != ConditionAction.Property.VISIBILITY) {
			throw new BadRequestException(
					target.getComponentType() + " components can only be the target of SHOW or HIDE");
		}
		for (ConditionRequest.Rule rule : request.rules()) {
			requireValidRule(rule, target, byCode);
		}
		requireNoConditionForSameProperty(others, request);
		requireNoCycle(components, others, request);

		condition.setTargetFieldCode(request.targetFieldCode());
		condition.setAction(request.action());
		condition.setLogicalOperator(request.logicalOperator());
		condition.getRules().clear();
		for (ConditionRequest.Rule rule : request.rules()) {
			ConditionRule entity = new ConditionRule();
			entity.setSourceFieldCode(rule.sourceFieldCode());
			entity.setOperator(rule.operator());
			entity.setValue(rule.operator().needsValue() ? rule.value().trim() : null);
			condition.getRules().add(entity);
		}
		return ConditionResponse.from(conditionRepository.saveAndFlush(condition));
	}

	private void requireValidRule(ConditionRequest.Rule rule, FormComponent target, Map<String, FormComponent> byCode) {
		FormComponent source = byCode.get(rule.sourceFieldCode());
		if (source == null) {
			throw new BadRequestException("Source field '" + rule.sourceFieldCode() + "' does not exist in this form");
		}
		if (source.getId().equals(target.getId())) {
			throw new BadRequestException("A field cannot depend on itself");
		}
		ComponentType type = source.getComponentType();
		if (type.isLayout()) {
			throw new BadRequestException(type + " components cannot be used as a condition source");
		}
		if (!operatorFits(rule.operator(), type)) {
			throw new BadRequestException(
					rule.operator() + " cannot be used with " + type + " field '" + source.getFieldCode() + "'");
		}
		if (!rule.operator().needsValue()) {
			return;
		}
		if (rule.value() == null || rule.value().isBlank()) {
			throw new BadRequestException("A value is required for " + rule.operator());
		}
		requireValueFitsType(rule.value().trim(), source);
	}

	private boolean operatorFits(ConditionOperator operator, ComponentType type) {
		return switch (operator) {
			case EQUALS, NOT_EQUALS -> !type.isMultiValue();
			case CONTAINS -> CONTAINS_TYPES.contains(type);
			case GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL -> ORDERED_TYPES.contains(type);
			case IS_EMPTY, IS_NOT_EMPTY -> true;
		};
	}

	private void requireValueFitsType(String value, FormComponent source) {
		ComponentType type = source.getComponentType();
		String expected = null;
		try {
			if (type == NUMBER || type == DECIMAL) {
				expected = "a number";
				new BigDecimal(value);
			} else if (type == DATE) {
				expected = "a date in the format yyyy-MM-dd";
				LocalDate.parse(value, AnswerFormat.DATE);
			} else if (type == DATETIME) {
				expected = "a date and time in the format yyyy-MM-ddTHH:mm";
				LocalDateTime.parse(value, AnswerFormat.DATE_TIME);
			} else if (type == TOGGLE && !value.equals("true") && !value.equals("false")) {
				throw new BadRequestException(
						"Value for " + type + " field '" + source.getFieldCode() + "' must be true or false");
			}
		} catch (NumberFormatException | DateTimeParseException ex) {
			throw new BadRequestException(
					"Value for " + type + " field '" + source.getFieldCode() + "' must be " + expected);
		}
	}

	private void requireNoConditionForSameProperty(List<FormCondition> others, ConditionRequest request) {
		for (FormCondition other : others) {
			if (other.getTargetFieldCode().equals(request.targetFieldCode())
					&& other.getAction().property() == request.action().property()) {
				throw new ConflictException("Field '" + request.targetFieldCode() + "' already has a "
						+ other.getAction() + " condition (#" + other.getId()
						+ "). Edit that condition instead of adding another");
			}
		}
	}

	private void requireNoCycle(List<FormComponent> components, List<FormCondition> others, ConditionRequest request) {
		Set<String> sources = request.rules().stream().map(ConditionRequest.Rule::sourceFieldCode)
				.collect(Collectors.toSet());
		String loopsBackTo = firstReached(influences(components, others), request.targetFieldCode(), sources);
		if (loopsBackTo != null) {
			throw new BadRequestException("This condition creates a circular dependency: '"
					+ request.targetFieldCode() + "' would depend on '" + loopsBackTo
					+ "', which already depends on '" + request.targetFieldCode() + "'");
		}
	}

	/**
	 * Moving a field into a section makes it depend on that section's visibility. That is a loop if the
	 * section's own visibility already depends on the field.
	 */
	@Transactional(readOnly = true)
	public void requireMoveCreatesNoCycle(Long formId, FormComponent moved, FormComponent newSection) {
		List<FormComponent> components = componentRepository.findByFormIdOrderByDisplayOrderAscIdAsc(formId);
		List<FormCondition> conditions = conditionRepository.findByFormIdOrderByIdAsc(formId);
		if (firstReached(influences(components, conditions), moved.getFieldCode(),
				Set.of(newSection.getFieldCode())) != null) {
			throw new BadRequestException("'" + moved.getFieldCode() + "' cannot be moved into section '"
					+ newSection.getFieldCode() + "' because a condition on that section depends on it");
		}
	}

	/**
	 * "X influences Y" links: every condition (source -> target) and every section to its children
	 * (hiding a section hides them).
	 */
	private Map<String, Set<String>> influences(List<FormComponent> components, List<FormCondition> conditions) {
		Map<String, Set<String>> influences = new HashMap<>();
		Map<Long, String> codeById = components.stream()
				.collect(Collectors.toMap(FormComponent::getId, FormComponent::getFieldCode));
		for (FormComponent component : components) {
			if (component.getParentSectionId() != null) {
				link(influences, codeById.get(component.getParentSectionId()), component.getFieldCode());
			}
		}
		for (FormCondition condition : conditions) {
			condition.getRules()
					.forEach(rule -> link(influences, rule.getSourceFieldCode(), condition.getTargetFieldCode()));
		}
		return influences;
	}

	private String firstReached(Map<String, Set<String>> influences, String start, Set<String> lookingFor) {
		Set<String> seen = new HashSet<>();
		Deque<String> toVisit = new ArrayDeque<>(List.of(start));
		while (!toVisit.isEmpty()) {
			String current = toVisit.pop();
			if (!seen.add(current)) {
				continue;
			}
			if (lookingFor.contains(current)) {
				return current;
			}
			toVisit.addAll(influences.getOrDefault(current, Set.of()));
		}
		return null;
	}

	private void link(Map<String, Set<String>> influences, String from, String to) {
		if (!Objects.equals(from, to)) {
			influences.computeIfAbsent(from, key -> new HashSet<>()).add(to);
		}
	}
}
