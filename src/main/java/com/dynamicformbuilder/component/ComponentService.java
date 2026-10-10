package com.dynamicformbuilder.component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dynamicformbuilder.common.exception.BadRequestException;
import com.dynamicformbuilder.common.exception.ConflictException;
import com.dynamicformbuilder.common.exception.ResourceNotFoundException;
import com.dynamicformbuilder.component.ComponentProperties.Option;
import com.dynamicformbuilder.condition.ConditionRepository;
import com.dynamicformbuilder.condition.ConditionService;
import com.dynamicformbuilder.condition.FormCondition;
import com.dynamicformbuilder.form.FormService;

import tools.jackson.databind.JsonNode;

@Service
public class ComponentService {

	private static final String STATIC_DATA_SOURCE = "STATIC";
	private static final int DEFAULT_DECIMAL_PLACES = 2;
	private static final int MAX_DECIMAL_PLACES = 10;

	private final ComponentRepository componentRepository;
	private final ComponentResponseAssembler assembler;
	private final ConditionRepository conditionRepository;
	private final ConditionService conditionService;
	private final FormService formService;

	public ComponentService(ComponentRepository componentRepository, ComponentResponseAssembler assembler,
			ConditionRepository conditionRepository, ConditionService conditionService, FormService formService) {
		this.componentRepository = componentRepository;
		this.assembler = assembler;
		this.conditionRepository = conditionRepository;
		this.conditionService = conditionService;
		this.formService = formService;
	}

	@Transactional
	public ComponentResponse create(Long formId, ComponentRequest request) {
		formService.getEditableForm(formId);
		if (componentRepository.existsByFormIdAndFieldCode(formId, request.fieldCode())) {
			throw new ConflictException("A component with this field code already exists in this form");
		}
		requireValidParent(formId, request.componentType(), request.parentSectionId(), null);

		FormComponent component = new FormComponent();
		component.setFormId(formId);
		component.setComponentType(request.componentType());
		component.setParentSectionId(request.parentSectionId());
		component.setDisplayOrder(nextDisplayOrder(formId, request.parentSectionId()));
		applyEditableFields(component, request);
		return assembler.one(componentRepository.save(component));
	}

	@Transactional
	public ComponentResponse update(Long formId, Long componentId, ComponentRequest request) {
		formService.getEditableForm(formId);
		FormComponent component = findComponent(formId, componentId);
		if (request.componentType() != component.getComponentType()) {
			throw new BadRequestException("Component type cannot be changed. Delete the component and add a new one");
		}
		if (!request.fieldCode().equals(component.getFieldCode())) {
			if (componentRepository.existsByFormIdAndFieldCode(formId, request.fieldCode())) {
				throw new ConflictException("A component with this field code already exists in this form");
			}
			requireNotUsedByConditions(component, "Its field code cannot be changed");
		}
		if (!Objects.equals(request.parentSectionId(), component.getParentSectionId())) {
			FormComponent newSection = requireValidParent(formId, component.getComponentType(),
					request.parentSectionId(), componentId);
			if (newSection != null) {
				conditionService.requireMoveCreatesNoCycle(formId, component, newSection);
			}
			int lastInNewParent = nextDisplayOrder(formId, request.parentSectionId());
			component.setParentSectionId(request.parentSectionId());
			component.setDisplayOrder(lastInNewParent);
		}

		applyEditableFields(component, request);
		return assembler.one(componentRepository.saveAndFlush(component));
	}

	@Transactional
	public void delete(Long formId, Long componentId) {
		formService.getEditableForm(formId);
		FormComponent component = findComponent(formId, componentId);
		if (componentRepository.existsByParentSectionId(componentId)) {
			throw new ConflictException("This section still contains components. Move or delete them first");
		}
		requireNotUsedByConditions(component, "It cannot be deleted");
		componentRepository.delete(component);
	}

	@Transactional
	public List<ComponentResponse> reorder(Long formId, ReorderComponentsRequest request) {
		formService.getEditableForm(formId);
		List<FormComponent> siblings = componentRepository
				.findByFormIdAndParentSectionIdOrderByDisplayOrderAscIdAsc(formId, request.parentSectionId());
		Map<Long, FormComponent> byId = siblings.stream()
				.collect(Collectors.toMap(FormComponent::getId, Function.identity()));

		List<Long> ids = request.componentIds();
		if (ids.size() != siblings.size() || !new HashSet<>(ids).equals(byId.keySet())) {
			throw new BadRequestException(
					"componentIds must list every component of this parent exactly once (" + siblings.size()
							+ " expected)");
		}

		for (int i = 0; i < ids.size(); i++) {
			byId.get(ids.get(i)).setDisplayOrder(i + 1);
		}
		componentRepository.saveAll(siblings);
		return assembler.flat(ids.stream().map(byId::get).toList());
	}

	public FormComponent findComponent(Long formId, Long componentId) {
		return componentRepository.findByIdAndFormId(componentId, formId)
				.orElseThrow(() -> new ResourceNotFoundException("Component not found"));
	}

	private void requireNotUsedByConditions(FormComponent component, String consequence) {
		List<FormCondition> conditions = conditionRepository.findUsingField(component.getFormId(),
				component.getFieldCode());
		if (!conditions.isEmpty()) {
			String ids = conditions.stream().map(condition -> "#" + condition.getId())
					.collect(Collectors.joining(", "));
			throw new ConflictException("Field '" + component.getFieldCode() + "' is used by condition " + ids
					+ ". " + consequence + " until those conditions are removed or changed");
		}
	}

	private void applyEditableFields(FormComponent component, ComponentRequest request) {
		ComponentType type = component.getComponentType();
		ComponentProperties properties = normaliseProperties(type, request.properties());

		component.setFieldCode(request.fieldCode());
		component.setFieldLabel(request.fieldLabel());
		component.setPlaceholder(request.placeholder());
		component.setHelpText(request.helpText());
		component.setRequired(Boolean.TRUE.equals(request.required()));
		component.setReadOnly(Boolean.TRUE.equals(request.readOnly()));
		component.setDisabled(Boolean.TRUE.equals(request.disabled()));
		component.setVisible(!Boolean.FALSE.equals(request.visible()));
		component.setProperties(properties);
		component.setDefaultValue(checkedDefaultValue(type, properties, request.defaultValue()));
	}

	private JsonNode checkedDefaultValue(ComponentType type, ComponentProperties properties, JsonNode value) {
		if (AnswerFormat.isEmpty(value)) {
			return null;
		}
		if (type.isLayout()) {
			throw new BadRequestException(type + " components cannot have a default value");
		}
		String problem = AnswerFormat.check(type, properties, value);
		if (problem != null) {
			throw new BadRequestException("Invalid default value: " + problem);
		}
		return value;
	}

	private ComponentProperties normaliseProperties(ComponentType type, ComponentProperties given) {
		ComponentProperties p = given != null ? given : new ComponentProperties(null, null, null, null);

		boolean choice = type.hasOptions();
		rejectUnless(choice, p.dataSource(), "dataSource", type);
		rejectUnless(choice, p.options(), "options", type);
		rejectUnless(type == ComponentType.DECIMAL, p.decimalPlaces(), "decimalPlaces", type);
		rejectUnless(type == ComponentType.SECTION, p.collapsible(), "collapsible", type);

		if (choice) {
			if (p.dataSource() != null && !STATIC_DATA_SOURCE.equals(p.dataSource())) {
				throw new BadRequestException("dataSource must be STATIC");
			}
			List<Option> options = p.options() != null ? p.options() : List.of();
			for (Option option : options) {
				if (option == null || isBlank(option.label()) || isBlank(option.value())) {
					throw new BadRequestException("Every option needs a label and a value");
				}
			}
			return new ComponentProperties(STATIC_DATA_SOURCE, options, null, null);
		}
		if (type == ComponentType.DECIMAL) {
			int places = p.decimalPlaces() != null ? p.decimalPlaces() : DEFAULT_DECIMAL_PLACES;
			if (places < 0 || places > MAX_DECIMAL_PLACES) {
				throw new BadRequestException("decimalPlaces must be between 0 and " + MAX_DECIMAL_PLACES);
			}
			return new ComponentProperties(null, null, places, null);
		}
		if (type == ComponentType.SECTION) {
			return new ComponentProperties(null, null, null, Boolean.TRUE.equals(p.collapsible()));
		}
		return new ComponentProperties(null, null, null, null);
	}

	private void rejectUnless(boolean allowed, Object value, String name, ComponentType type) {
		if (!allowed && value != null) {
			throw new BadRequestException("Property '" + name + "' is not supported on " + type + " components");
		}
	}

	private FormComponent requireValidParent(Long formId, ComponentType type, Long parentSectionId, Long selfId) {
		if (parentSectionId == null) {
			return null;
		}
		if (type == ComponentType.SECTION) {
			throw new BadRequestException("A section cannot be placed inside another section");
		}
		FormComponent parent = componentRepository.findByIdAndFormId(parentSectionId, formId)
				.orElseThrow(() -> new BadRequestException("Parent section not found in this form"));
		if (parent.getComponentType() != ComponentType.SECTION || parent.getId().equals(selfId)) {
			throw new BadRequestException("parentSectionId must refer to a SECTION component");
		}
		return parent;
	}

	private int nextDisplayOrder(Long formId, Long parentSectionId) {
		return componentRepository.findByFormIdAndParentSectionIdOrderByDisplayOrderAscIdAsc(formId, parentSectionId)
				.stream().mapToInt(FormComponent::getDisplayOrder).max().orElse(0) + 1;
	}

	private boolean isBlank(String text) {
		return text == null || text.isBlank();
	}
}
