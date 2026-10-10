package com.dynamicformbuilder.submission;

import java.time.LocalDateTime;

public record SubmissionConfirmationResponse(
		Long submissionId,
		String formCode,
		Integer version,
		LocalDateTime submittedAt) {
}
