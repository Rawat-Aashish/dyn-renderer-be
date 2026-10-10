package com.dynamicformbuilder.audit;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dynamicformbuilder.common.exception.ResourceNotFoundException;
import com.dynamicformbuilder.common.response.PageResponse;
import com.dynamicformbuilder.common.response.Paging;
import com.dynamicformbuilder.form.FormRepository;
import com.dynamicformbuilder.user.User;
import com.dynamicformbuilder.user.UserRepository;

@Service
public class AuditService {

	private final AuditRepository auditRepository;
	private final FormRepository formRepository;
	private final UserRepository userRepository;

	public AuditService(AuditRepository auditRepository, FormRepository formRepository,
			UserRepository userRepository) {
		this.auditRepository = auditRepository;
		this.formRepository = formRepository;
		this.userRepository = userRepository;
	}

	/**
	 * Called inside the caller's transaction, so the audit row is saved only if the action itself succeeds.
	 * The acting user is the email in the JWT of the current request.
	 */
	@Transactional
	public void record(Long formId, AuditAction action, Integer versionNumber) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		FormAudit audit = new FormAudit();
		audit.setFormId(formId);
		audit.setAction(action);
		audit.setVersionNumber(versionNumber);
		audit.setUserId(user.getId());
		auditRepository.save(audit);
	}

	@Transactional(readOnly = true)
	public PageResponse<AuditResponse> list(Long formId, int page, int size) {
		if (!formRepository.existsById(formId)) {
			throw new ResourceNotFoundException("Form not found");
		}
		Page<FormAudit> audits = auditRepository.findByFormId(formId, Paging.descending(page, size, "createdAt", "id"));
		Map<Long, User> users = userRepository
				.findAllById(audits.getContent().stream().map(FormAudit::getUserId).distinct().toList()).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
		return PageResponse.from(audits, audit -> AuditResponse.from(audit, users.get(audit.getUserId())));
	}
}
