package com.dynamicformbuilder.formversion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dynamicformbuilder.audit.AuditAction;
import com.dynamicformbuilder.audit.AuditService;
import com.dynamicformbuilder.common.exception.ConflictException;
import com.dynamicformbuilder.common.exception.FieldErrorsException;
import com.dynamicformbuilder.common.exception.ResourceNotFoundException;
import com.dynamicformbuilder.common.response.PageResponse;
import com.dynamicformbuilder.common.response.Paging;
import com.dynamicformbuilder.component.ComponentResponse;
import com.dynamicformbuilder.component.ComponentResponseAssembler;
import com.dynamicformbuilder.form.Form;
import com.dynamicformbuilder.form.FormRepository;
import com.dynamicformbuilder.form.FormService;
import com.dynamicformbuilder.form.FormStatus;

import tools.jackson.databind.ObjectMapper;

@Service
public class FormVersionService {

	private final FormVersionRepository formVersionRepository;
	private final FormRepository formRepository;
	private final FormService formService;
	private final ComponentResponseAssembler componentAssembler;
	private final PublishChecker publishChecker;
	private final AuditService auditService;
	private final ObjectMapper objectMapper;

	public FormVersionService(FormVersionRepository formVersionRepository, FormRepository formRepository,
			FormService formService, ComponentResponseAssembler componentAssembler, PublishChecker publishChecker,
			AuditService auditService, ObjectMapper objectMapper) {
		this.formVersionRepository = formVersionRepository;
		this.formRepository = formRepository;
		this.formService = formService;
		this.componentAssembler = componentAssembler;
		this.publishChecker = publishChecker;
		this.auditService = auditService;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public PageResponse<FormVersionResponse> list(Long formId, int page, int size) {
		formService.findForm(formId);
		return PageResponse.from(
				formVersionRepository.findByFormId(formId, Paging.descending(page, size, "versionNumber")),
				FormVersionResponse::from);
	}

	@Transactional(readOnly = true)
	public FormVersionDetailResponse get(Long formId, Integer versionNumber) {
		formService.findForm(formId);
		return FormVersionDetailResponse.from(findVersion(formId, versionNumber));
	}

	@Transactional
	public FormVersionResponse createNewVersion(Long formId) {
		Form form = formService.findForm(formId);
		formService.requireNotArchived(form);
		if (form.getStatus() != FormStatus.PUBLISHED) {
			throw new ConflictException(form.getPublishedVersion() == null
					? "Version " + form.getCurrentVersion() + " has not been published yet. Publish it before creating a new version"
					: "Version " + form.getCurrentVersion() + " is already a draft. Publish it before creating another");
		}

		int next = form.getCurrentVersion() + 1;
		FormVersion version = new FormVersion();
		version.setFormId(formId);
		version.setVersionNumber(next);
		version.setStatus(FormVersionStatus.DRAFT);
		formVersionRepository.save(version);

		form.setCurrentVersion(next);
		form.setStatus(FormStatus.DRAFT);
		formRepository.save(form);

		auditService.record(formId, AuditAction.VERSION_CREATED, next);
		return FormVersionResponse.from(version);
	}

	@Transactional
	public FormVersionDetailResponse publish(Long formId, Integer versionNumber) {
		Form form = formService.findForm(formId);
		formService.requireNotArchived(form);
		FormVersion version = findVersion(formId, versionNumber);
		if (version.getStatus() != FormVersionStatus.DRAFT) {
			throw new ConflictException("Version " + versionNumber + " is " + version.getStatus()
					+ " and cannot be published again");
		}

		List<ComponentResponse> components = componentAssembler.tree(formId);
		Map<String, String> problems = publishChecker.check(components);
		if (!problems.isEmpty()) {
			throw new FieldErrorsException("The form cannot be published until these problems are fixed", problems);
		}

		formVersionRepository.findByFormIdAndStatus(formId, FormVersionStatus.PUBLISHED).ifPresent(live -> {
			live.setStatus(FormVersionStatus.SUPERSEDED);
			formVersionRepository.saveAndFlush(live);
		});

		FormConfiguration configuration = new FormConfiguration(form.getName(), form.getCode(),
				form.getDescription(), form.getCategory(), versionNumber, components, formService.conditions(formId));
		version.setConfiguration(objectMapper.valueToTree(configuration));
		version.setStatus(FormVersionStatus.PUBLISHED);
		version.setPublishedAt(LocalDateTime.now());
		formVersionRepository.save(version);

		form.setStatus(FormStatus.PUBLISHED);
		form.setPublishedVersion(versionNumber);
		formRepository.save(form);

		auditService.record(formId, AuditAction.VERSION_PUBLISHED, versionNumber);
		return FormVersionDetailResponse.from(version);
	}

	private FormVersion findVersion(Long formId, Integer versionNumber) {
		return formVersionRepository.findByFormIdAndVersionNumber(formId, versionNumber)
				.orElseThrow(() -> new ResourceNotFoundException("Version not found"));
	}
}
