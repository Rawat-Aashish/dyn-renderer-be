package com.dynamicformbuilder.submission;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

import org.springframework.web.multipart.MultipartFile;

import com.dynamicformbuilder.component.ComponentResponse;
import com.dynamicformbuilder.component.ComponentType;

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
	private static final BigDecimal MIN_RATING = new BigDecimal("0.5");
	private static final BigDecimal MAX_RATING = new BigDecimal("5");

	private final SubmissionRepository submissionRepository;
	private final FormRepository formRepository;
	private final FormVersionRepository formVersionRepository;
	private final ObjectMapper objectMapper;
	private final SubmissionFileService fileService;

	public SubmissionService(SubmissionRepository submissionRepository, FormRepository formRepository,
			FormVersionRepository formVersionRepository, ObjectMapper objectMapper,
			SubmissionFileService fileService) {
		this.fileService = fileService;
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
		FormSubmission submission = startedSubmission(form, request.token());

		JsonNode answers = request.data() == null || request.data().isNull()
				? JsonNodeFactory.instance.objectNode() : request.data();
		if (!answers.isObject()) {
			throw new BadRequestException("data must be an object of field codes and values");
		}

		FormVersion version = versionOf(submission);
		Map<String, JsonNode> files = fileService.answers(submission.getId());
		AnswerEvaluator evaluator = new AnswerEvaluator(configurationOf(version), answers, files);
		evaluator.evaluate();
		if (!evaluator.errors().isEmpty()) {
			throw new FieldErrorsException("Validation failed", evaluator.errors());
		}

		// An upload whose field ended up hidden, disabled or unanswered must not outlive the submission
		fileService.removeUnused(submission.getId(), files.keySet().stream()
				.filter(evaluator.cleanedData()::has)
				.collect(Collectors.toSet()));
		submission.setSubmittedData(evaluator.cleanedData());
		submission.setStatus(SubmissionStatus.SUBMITTED);
		submission.setSubmittedAt(LocalDateTime.now());
		submissionRepository.save(submission);

		return new SubmissionConfirmationResponse(submission.getId(), form.getCode(), version.getVersionNumber(),
				submission.getSubmittedAt());
	}

	/**
	 * The optional last step: the person rates the form they have just submitted. Allowed once per submission,
	 * and only if the version they filled in was published with the rating switched on.
	 */
	@Transactional
	public void rate(String formCode, RateSubmissionRequest request) {
		Form form = findByCode(formCode);
		requireOpen(form);
		FormSubmission submission = submissionRepository.findByToken(request.token())
				.filter(found -> found.getFormId().equals(form.getId()))
				.orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
		if (submission.getStatus() != SubmissionStatus.SUBMITTED) {
			throw new ConflictException("Submit the form before rating it");
		}
		if (!Boolean.TRUE.equals(configurationOf(versionOf(submission)).ratingEnabled())) {
			throw new ConflictException("This form does not take ratings");
		}
		if (submission.getRating() != null) {
			throw new ConflictException("This submission has already been rated");
		}
		BigDecimal rating = request.rating();
		if (rating.compareTo(MIN_RATING) < 0 || rating.compareTo(MAX_RATING) > 0
				|| rating.remainder(MIN_RATING).signum() != 0) {
			throw new FieldErrorsException("Validation failed",
					Map.of("rating", "Rating must be between 0.5 and 5 in steps of 0.5"));
		}
		submission.setRating(rating);
		submissionRepository.save(submission);
	}

	@Transactional
	public SubmissionFileResponse uploadFile(String formCode, UUID token, String fieldCode, MultipartFile file) {
		Form form = findByCode(formCode);
		requireOpen(form);
		FormSubmission submission = startedSubmission(form, token);
		ComponentResponse field = configurationOf(versionOf(submission)).components().stream()
				.flatMap(top -> Stream.concat(Stream.of(top), top.children().stream()))
				.filter(component -> component.componentType() == ComponentType.FILE_UPLOAD
						&& component.fieldCode().equals(fieldCode))
				.findFirst()
				.orElseThrow(() -> new BadRequestException("'" + fieldCode + "' is not a file upload field of this form"));
		return fileService.store(submission, field, file);
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

		Double averageRating = submissionRepository.averageRating(form.getId());
		return new SubmissionListResponse(form.getId(), form.getCode(), form.getName(), form.getDeletedAt() != null,
				submissionRepository.countByFormId(form.getId()),
				submissionRepository.countByFormIdAndStatus(form.getId(), SubmissionStatus.SUBMITTED),
				averageRating == null ? null : BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP),
				submissionRepository.countByFormIdAndRatingIsNotNull(form.getId()),
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

	private FormSubmission startedSubmission(Form form, UUID token) {
		FormSubmission submission = submissionRepository.findByToken(token)
				.filter(found -> found.getFormId().equals(form.getId()))
				.orElseThrow(() -> new ResourceNotFoundException("Submission not found. Start the form again"));
		if (submission.getStatus() == SubmissionStatus.SUBMITTED) {
			throw new ConflictException("This form has already been submitted");
		}
		return submission;
	}

	// The version the user started with, even if a newer one has been published since
	private FormVersion versionOf(FormSubmission submission) {
		return formVersionRepository.findById(submission.getFormVersionId())
				.orElseThrow(() -> new ResourceNotFoundException("Version not found"));
	}

	private FormConfiguration configurationOf(FormVersion version) {
		return objectMapper.treeToValue(version.getConfiguration(), FormConfiguration.class);
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
