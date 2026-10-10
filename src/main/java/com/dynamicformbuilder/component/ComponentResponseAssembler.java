package com.dynamicformbuilder.component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.dynamicformbuilder.validation.ValidationRule;
import com.dynamicformbuilder.validation.ValidationRuleRepository;
import com.dynamicformbuilder.validation.ValidationRuleResponse;

/**
 * Turns component entities into responses with their validation rules attached, loading the rules in one
 * query. Lives outside ComponentService so FormService can use it without the two services depending on
 * each other.
 */
@Component
public class ComponentResponseAssembler {

	private final ComponentRepository componentRepository;
	private final ValidationRuleRepository validationRuleRepository;

	public ComponentResponseAssembler(ComponentRepository componentRepository,
			ValidationRuleRepository validationRuleRepository) {
		this.componentRepository = componentRepository;
		this.validationRuleRepository = validationRuleRepository;
	}

	public ComponentResponse one(FormComponent component) {
		return flat(List.of(component)).getFirst();
	}

	public List<ComponentResponse> flat(List<FormComponent> components) {
		Map<Long, List<ValidationRuleResponse>> rules = rulesByComponent(components);
		return components.stream()
				.map(component -> toResponse(component, rules, List.of()))
				.toList();
	}

	/** Top-level components in display order, each section carrying its children. */
	public List<ComponentResponse> tree(Long formId) {
		List<FormComponent> all = componentRepository.findByFormIdOrderByDisplayOrderAscIdAsc(formId);
		Map<Long, List<ValidationRuleResponse>> rules = rulesByComponent(all);
		Map<Long, List<ComponentResponse>> childrenByParent = all.stream()
				.filter(component -> component.getParentSectionId() != null)
				.collect(Collectors.groupingBy(FormComponent::getParentSectionId,
						Collectors.mapping(component -> toResponse(component, rules, List.of()),
								Collectors.toList())));
		return all.stream()
				.filter(component -> component.getParentSectionId() == null)
				.map(component -> toResponse(component, rules,
						childrenByParent.getOrDefault(component.getId(), List.of())))
				.toList();
	}

	private Map<Long, List<ValidationRuleResponse>> rulesByComponent(List<FormComponent> components) {
		List<Long> ids = components.stream().map(FormComponent::getId).toList();
		return validationRuleRepository.findByComponentIdInOrderByIdAsc(ids).stream()
				.collect(Collectors.groupingBy(ValidationRule::getComponentId,
						Collectors.mapping(ValidationRuleResponse::from, Collectors.toList())));
	}

	private ComponentResponse toResponse(FormComponent component, Map<Long, List<ValidationRuleResponse>> rules,
			List<ComponentResponse> children) {
		return ComponentResponse.from(component, rules.getOrDefault(component.getId(), List.of()), children);
	}
}
