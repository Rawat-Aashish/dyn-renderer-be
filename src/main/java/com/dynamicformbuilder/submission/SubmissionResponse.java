package com.dynamicformbuilder.submission;

import java.time.LocalDateTime;

import com.dynamicformbuilder.form.Form;

import tools.jackson.databind.JsonNode;

public record SubmissionResponse(
		Long id,
		Long formId,
		String formCode,
		String formName,
		boolean archived,
		Integer version,
		LocalDateTime startedAt,
		LocalDateTime submittedAt,
		JsonNode data) {

	public static SubmissionResponse from(FormSubmission submission, Form form, Integer versionNumber) {
		return new SubmissionResponse(submission.getId(), form.getId(), form.getCode(), form.getName(),
				form.getDeletedAt() != null, versionNumber, submission.getStartedAt(), submission.getSubmittedAt(),
				submission.getSubmittedData());
	}
}
