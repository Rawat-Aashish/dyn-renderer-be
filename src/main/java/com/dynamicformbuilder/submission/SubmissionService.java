package com.dynamicformbuilder.submission;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dynamicformbuilder.common.exception.BadRequestException;
import com.dynamicformbuilder.common.exception.ConflictException;
import com.dynamicformbuilder.common.exception.FieldErrorsException;
import com.dynamicformbuilder.common.exception.GoneException;
import com.dynamicformbuilder.common.exception.ResourceNotFoundException;
import com.dynamicformbuilder.common.response.Paging;
import com.dynamicformbuilder.form.Form;
import com.dynamicformbuilder.form.FormRepository;
import com.dynamicformbuilder.formversion.FormConfiguration;
import com.dynamicformbuilder.formversion.FormVersion;
import com.dynamicformbuilder.formversion.FormVersionRepository;
import com.dynamicformbuilder.formversion.FormVersionStatus;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

@Service
public class SubmissionService {

	private static final String FORM_CLOSED = "This form is no longer accepting responses";

	private final SubmissionRepository submissionRepository;
	private final FormRepository formRepository;
	private final FormVersionRepository formVersionRepository;
	private final ObjectMapper objectMapper;

	public SubmissionService(SubmissionRepository submissionRepository, FormRepository formRepository,
			FormVersionRepository formVersionRepository, ObjectMapper objectMapper) {
		this.submissionRepository = submissionRepository;
		this.formRepository = formRepository;
		this.formVersionRepository = formVersionRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public PublishedFormResponse getPublished(String formCode) {
		Form form = findByCode(formCode);
		if (form.getDeletedAt() != null) {
			throw new ResourceNotFoundException("Form not found");
		}
		FormVersion live = liveVersion(form);
		return new PublishedFormResponse(form.getCode(), live.getVersionNumber(), live.getConfiguration());
	}

	@Transactional
	public StartSubmissionResponse start(String formCode) {
		Form form = findByCode(formCode);
		requireOpen(form);
		FormVersion live = liveVersion(form);

		FormSubmission submission = new FormSubmission();
		submission.setFormId(form.getId());
		submission.setFormVersionId(live.getId());
		submission.setToken(UUID.randomUUID());
		submission.setStatus(SubmissionStatus.STARTED);
		submission.setStartedAt(LocalDateTime.now());
		submissionRepository.save(submission);

		return new StartSubmissionResponse(submission.getToken(), form.getCode(), live.getVersionNumber(),
				submission.getStartedAt(), live.getConfiguration());
	}

	@Transactional
	public SubmissionConfirmationResponse submit(String formCode, SubmitFormRequest request) {
		Form form = findByCode(formCode);
		requireOpen(form);
		FormSubmission submission = submissionRepository.findByToken(request.token())
				.filter(found -> found.getFormId().equals(form.getId()))
				.orElseThrow(() -> new ResourceNotFoundException("Submission not found. Start the form again"));
		if (submission.getStatus() == SubmissionStatus.SUBMITTED) {
			throw new ConflictException("This form has already been submitted");
		}

		JsonNode answers = request.data() == null || request.data().isNull()
				? JsonNodeFactory.instance.objectNode() : request.data();
		if (!answers.isObject()) {
			throw new BadRequestException("data must be an object of field codes and values");
		}

		// The version the user started with, even if a newer one has been published since
		FormVersion version = formVersionRepository.findById(submission.getFormVersionId())
				.orElseThrow(() -> new ResourceNotFoundException("Version not found"));
		FormConfiguration configuration = objectMapper.treeToValue(version.getConfiguration(), FormConfiguration.class);

		AnswerEvaluator evaluator = new AnswerEvaluator(configuration, answers);
		evaluator.evaluate();
		if (!evaluator.errors().isEmpty()) {
			throw new FieldErrorsException("Validation failed", evaluator.errors());
		}

		submission.setSubmittedData(evaluator.cleanedData());
		submission.setStatus(SubmissionStatus.SUBMITTED);
		submission.setSubmittedAt(LocalDateTime.now());
		submissionRepository.save(submission);

		return new SubmissionConfirmationResponse(submission.getId(), form.getCode(), version.getVersionNumber(),
				submission.getSubmittedAt());
	}

	@Transactional(readOnly = true)
	public SubmissionListResponse list(String formCode, Integer versionNumber, int page, int size) {
		Form form = findByCode(formCode);
		Pageable pageable = Paging.descending(page, size, "submittedAt", "id");

		Page<FormSubmission> submissions;
		if (versionNumber == null) {
			submissions = submissionRepository.findByFormIdAndStatus(form.getId(), SubmissionStatus.SUBMITTED, pageable);
		} else {
			FormVersion version = formVersionRepository.findByFormIdAndVersionNumber(form.getId(), versionNumber)
					.orElseThrow(() -> new ResourceNotFoundException("Version not found"));
			submissions = submissionRepository.findByFormIdAndFormVersionIdAndStatus(form.getId(), version.getId(),
					SubmissionStatus.SUBMITTED, pageable);
		}

		Map<Long, Integer> versionNumbers = formVersionRepository
				.findAllById(submissions.getContent().stream().map(FormSubmission::getFormVersionId).distinct().toList())
				.stream().collect(Collectors.toMap(FormVersion::getId, FormVersion::getVersionNumber));

		return new SubmissionListResponse(form.getId(), form.getCode(), form.getName(), form.getDeletedAt() != null,
				submissionRepository.countByFormId(form.getId()),
				submissionRepository.countByFormIdAndStatus(form.getId(), SubmissionStatus.SUBMITTED),
				submissions.getContent().stream()
						.map(submission -> SubmissionResponse.from(submission, form,
								versionNumbers.get(submission.getFormVersionId())))
						.toList(),
				submissions.getNumber(), submissions.getSize(), submissions.getTotalElements(),
				submissions.getTotalPages());
	}

	@Transactional(readOnly = true)
	public SubmissionResponse get(Long submissionId) {
		FormSubmission submission = submissionRepository.findByIdAndStatus(submissionId, SubmissionStatus.SUBMITTED)
				.orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
		Form form = formRepository.findById(submission.getFormId())
				.orElseThrow(() -> new ResourceNotFoundException("Form not found"));
		Integer versionNumber = formVersionRepository.findById(submission.getFormVersionId())
				.map(FormVersion::getVersionNumber)
				.orElse(null);
		return SubmissionResponse.from(submission, form, versionNumber);
	}

	private Form findByCode(String formCode) {
		return formRepository.findByCode(formCode).orElseThrow(() -> new ResourceNotFoundException("Form not found"));
	}

	private void requireOpen(Form form) {
		if (form.getDeletedAt() != null) {
			throw new GoneException(FORM_CLOSED);
		}
	}

	private FormVersion liveVersion(Form form) {
		return formVersionRepository.findByFormIdAndStatus(form.getId(), FormVersionStatus.PUBLISHED)
				.orElseThrow(() -> new ResourceNotFoundException("This form has not been published"));
	}
}
