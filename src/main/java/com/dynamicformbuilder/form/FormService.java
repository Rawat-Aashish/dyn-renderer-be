package com.dynamicformbuilder.form;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dynamicformbuilder.audit.AuditAction;
import com.dynamicformbuilder.audit.AuditService;
import com.dynamicformbuilder.common.exception.BadRequestException;
import com.dynamicformbuilder.common.exception.ConflictException;
import com.dynamicformbuilder.common.exception.ResourceNotFoundException;
import com.dynamicformbuilder.common.response.PageResponse;
import com.dynamicformbuilder.common.response.Paging;
import com.dynamicformbuilder.component.ComponentResponseAssembler;
import com.dynamicformbuilder.condition.ConditionRepository;
import com.dynamicformbuilder.condition.ConditionResponse;
import com.dynamicformbuilder.formversion.FormVersion;
import com.dynamicformbuilder.formversion.FormVersionRepository;
import com.dynamicformbuilder.formversion.FormVersionStatus;

@Service
public class FormService {

	private final FormRepository formRepository;
	private final FormVersionRepository formVersionRepository;
	private final ComponentResponseAssembler componentAssembler;
	private final ConditionRepository conditionRepository;
	private final AuditService auditService;

	public FormService(FormRepository formRepository, FormVersionRepository formVersionRepository,
			ComponentResponseAssembler componentAssembler, ConditionRepository conditionRepository,
			AuditService auditService) {
		this.formRepository = formRepository;
		this.formVersionRepository = formVersionRepository;
		this.componentAssembler = componentAssembler;
		this.conditionRepository = conditionRepository;
		this.auditService = auditService;
	}

	@Transactional
	public FormResponse create(CreateFormRequest request) {
		if (formRepository.existsByName(request.name())) {
			throw new ConflictException("A form with this name already exists");
		}
		if (formRepository.existsByCode(request.code())) {
			throw new ConflictException("A form with this code already exists");
		}

		Form form = new Form();
		form.setName(request.name());
		form.setCode(request.code());
		form.setDescription(request.description());
		form.setCategory(request.category());
		form.setStatus(FormStatus.DRAFT);
		form.setCurrentVersion(1);
		formRepository.save(form);

		FormVersion version = new FormVersion();
		version.setFormId(form.getId());
		version.setVersionNumber(1);
		version.setStatus(FormVersionStatus.DRAFT);
		formVersionRepository.save(version);

		auditService.record(form.getId(), AuditAction.FORM_CREATED, 1);
		return FormResponse.from(form);
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
