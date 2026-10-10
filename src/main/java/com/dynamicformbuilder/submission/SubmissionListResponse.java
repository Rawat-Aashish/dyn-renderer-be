package com.dynamicformbuilder.submission;

import java.util.List;

public record SubmissionListResponse(
		Long formId,
		String formCode,
		String formName,
		boolean archived,
		long startedCount,
		long submittedCount,
		List<SubmissionResponse> items,
		int page,
		int size,
		long totalItems,
		int totalPages) {
}
