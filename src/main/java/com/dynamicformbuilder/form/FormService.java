package com.dynamicformbuilder.form;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dynamicformbuilder.audit.AuditAction;
import com.dynamicformbuilder.audit.AuditService;
import com.dynamicformbuilder.common.exception.BadRequestException;
import com.dynamicformbuilder.common.exception.ConflictException;
import com.dynamicformbuilder.common.exception.ResourceNotFoundException;
import com.dynamicformbuilder.common.response.PageResponse;
import com.dynamicformbuilder.common.response.Paging;
import com.dynamicformbuilder.component.ComponentRepository;
import com.dynamicformbuilder.component.ComponentResponseAssembler;
import com.dynamicformbuilder.component.FormComponent;
import com.dynamicformbuilder.condition.ConditionRepository;
import com.dynamicformbuilder.condition.ConditionResponse;
import com.dynamicformbuilder.condition.ConditionRule;
import com.dynamicformbuilder.condition.FormCondition;
import com.dynamicformbuilder.formversion.FormVersion;
import com.dynamicformbuilder.formversion.FormVersionRepository;
import com.dynamicformbuilder.formversion.FormVersionStatus;
import com.dynamicformbuilder.validation.ValidationRule;
import com.dynamicformbuilder.validation.ValidationRuleRepository;

@Service
public class FormService {

	private final FormRepository formRepository;
	private final FormVersionRepository formVersionRepository;
	private final ComponentResponseAssembler componentAssembler;
	private final ConditionRepository conditionRepository;
	private final AuditService auditService;
	private final ComponentRepository componentRepository;
	private final ValidationRuleRepository validationRuleRepository;

	public FormService(FormRepository formRepository, FormVersionRepository formVersionRepository,
			ComponentResponseAssembler componentAssembler, ConditionRepository conditionRepository,
			AuditService auditService, ComponentRepository componentRepository,
			ValidationRuleRepository validationRuleRepository) {
		this.componentRepository = componentRepository;
		this.validationRuleRepository = validationRuleRepository;
		this.formRepository = formRepository;
		this.formVersionRepository = formVersionRepository;
		this.componentAssembler = componentAssembler;
		this.conditionRepository = conditionRepository;
		this.auditService = auditService;
	}

	@Transactional
	public FormResponse create(CreateFormRequest request) {
		return FormResponse.from(createDraft(request.name(), request.code(), request.description(),
				request.category(), Boolean.TRUE.equals(request.ratingEnabled()), AuditAction.FORM_CREATED));
	}

	/**
	 * Copies the source form's working copy (components, validation rules, conditions) into a new draft form.
	 * The rating setting is copied too. Versions, submissions and audit rows are not copied, and the source may be in any state, even archived.
	 */
	@Transactional
	public FormResponse cloneForm(Long sourceId, CloneFormRequest request) {
		Form source = findForm(sourceId);
		Form form = createDraft(request.name(), request.code(), source.getDescription(), source.getCategory(),
				source.isRatingEnabled(), AuditAction.FORM_CLONED);

		// Sections are saved before their children so a child can point at its new section's id
		List<FormComponent> components = componentRepository.findByFormIdOrderByDisplayOrderAscIdAsc(sourceId).stream()
				.sorted(Comparator.comparing(component -> component.getParentSectionId() != null))
				.toList();
		Map<Long, Long> newIds = new HashMap<>();
		for (FormComponent component : components) {
			FormComponent copy = new FormComponent();
			copy.setFormId(form.getId());
			copy.setFieldCode(component.getFieldCode());
			copy.setFieldLabel(component.getFieldLabel());
			copy.setComponentType(component.getComponentType());
			copy.setPlaceholder(component.getPlaceholder());
			copy.setHelpText(component.getHelpText());
			copy.setDefaultValue(component.getDefaultValue());
			copy.setDisplayOrder(component.getDisplayOrder());
			copy.setRequired(component.isRequired());
			copy.setReadOnly(component.isReadOnly());
			copy.setDisabled(component.isDisabled());
			copy.setVisible(component.isVisible());
			copy.setParentSectionId(newIds.get(component.getParentSectionId()));
			copy.setProperties(component.getProperties());
			newIds.put(component.getId(), componentRepository.save(copy).getId());
		}

		for (ValidationRule rule : validationRuleRepository.findByComponentIdInOrderByIdAsc(newIds.keySet())) {
			ValidationRule copy = new ValidationRule();
			copy.setComponentId(newIds.get(rule.getComponentId()));
			copy.setRuleType(rule.getRuleType());
			copy.setRuleValue(rule.getRuleValue());
			copy.setErrorMessage(rule.getErrorMessage());
			validationRuleRepository.save(copy);
		}

		// Conditions name their fields by fieldCode, which the copy keeps, so nothing needs remapping
		for (FormCondition condition : conditionRepository.findByFormIdOrderByIdAsc(sourceId)) {
			FormCondition copy = new FormCondition();
			copy.setFormId(form.getId());
			copy.setTargetFieldCode(condition.getTargetFieldCode());
			copy.setAction(condition.getAction());
			copy.setLogicalOperator(condition.getLogicalOperator());
			for (ConditionRule rule : condition.getRules()) {
				ConditionRule ruleCopy = new ConditionRule();
				ruleCopy.setSourceFieldCode(rule.getSourceFieldCode());
				ruleCopy.setOperator(rule.getOperator());
				ruleCopy.setValue(rule.getValue());
				copy.getRules().add(ruleCopy);
			}
			conditionRepository.save(copy);
		}
		return FormResponse.from(form);
	}

	private Form createDraft(String name, String code, String description, String category, boolean ratingEnabled,
			AuditAction action) {
		if (formRepository.existsByName(name)) {
			throw new ConflictException("A form with this name already exists");
		}
		if (formRepository.existsByCode(code)) {
			throw new ConflictException("A form with this code already exists");
		}

		Form form = new Form();
		form.setName(name);
		form.setCode(code);
		form.setDescription(description);
		form.setCategory(category);
		form.setRatingEnabled(ratingEnabled);
		form.setStatus(FormStatus.DRAFT);
		form.setCurrentVersion(1);
		formRepository.save(form);

		FormVersion version = new FormVersion();
		version.setFormId(form.getId());
		version.setVersionNumber(1);
		version.setStatus(FormVersionStatus.DRAFT);
		formVersionRepository.save(version);

		auditService.record(form.getId(), action, 1);
		return form;
	}

	@Transactional(readOnly = true)
	public PageResponse<FormResponse> list(boolean archived, FormStatus status, String search, int page, int size) {
		String pattern = "%" + (search == null ? "" : search.trim().toLowerCase()) + "%";
		var pageable = Paging.descending(page, size, "createdAt", "id");
		return PageResponse.from(formRepository.search(archived, status, pattern, pageable), FormResponse::from);
	}

	@Transactional(readOnly = true)
	public FormDetailResponse get(Long id) {
		return FormDetailResponse.from(findForm(id), componentAssembler.tree(id), conditions(id));
	}

	public List<ConditionResponse> conditions(Long formId) {
		return conditionRepository.findByFormIdOrderByIdAsc(formId).stream()
				.map(ConditionResponse::from)
				.toList();
	}

	@Transactional
	public FormResponse update(Long id, UpdateFormRequest request) {
		Form form = getEditableForm(id);
		if (request.code() != null && !request.code().equals(form.getCode())) {
			throw new BadRequestException("Form code cannot be changed");
		}
		if (formRepository.existsByNameAndIdNot(request.name(), id)) {
			throw new ConflictException("A form with this name already exists");
		}

		form.setName(request.name());
		form.setDescription(request.description());
		form.setCategory(request.category());
		form.setRatingEnabled(Boolean.TRUE.equals(request.ratingEnabled()));
		formRepository.saveAndFlush(form);
		auditService.record(id, AuditAction.FORM_UPDATED, form.getCurrentVersion());
		return FormResponse.from(form);
	}

	@Transactional
	public void delete(Long id) {
		Form form = findForm(id);
		requireNotArchived(form);
		if (form.getStatus() == FormStatus.PUBLISHED || form.getCurrentVersion() > 1) {
			throw new ConflictException("A form that has been published cannot be deleted. Archive it instead");
		}
		formRepository.delete(form);
	}

	@Transactional
	public FormResponse archive(Long id) {
		Form form = findForm(id);
		requireNotArchived(form);
		form.setDeletedAt(LocalDateTime.now());
		formRepository.saveAndFlush(form);
		auditService.record(id, AuditAction.FORM_ARCHIVED, form.getCurrentVersion());
		return FormResponse.from(form);
	}

	public Form findForm(Long id) {
		return formRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Form not found"));
	}

	public Form getEditableForm(Long id) {
		Form form = findForm(id);
		requireNotArchived(form);
		if (form.getStatus() != FormStatus.DRAFT) {
			throw new ConflictException("This form is published. Create a new version before making changes");
		}
		return form;
	}

	public void requireNotArchived(Form form) {
		if (form.getDeletedAt() != null) {
			throw new ConflictException("This form is archived and can no longer be changed");
		}
	}
}
