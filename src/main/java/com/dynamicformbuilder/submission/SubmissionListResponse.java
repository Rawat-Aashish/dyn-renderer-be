package com.dynamicformbuilder.submission;

import java.math.BigDecimal;

import java.util.List;

public record SubmissionListResponse(
		Long formId,
		String formCode,
		String formName,
		boolean archived,
		long startedCount,
		long submittedCount,
		BigDecimal averageRating,
		long ratingCount,
		List<SubmissionResponse> items,
		int page,
		int size,
		long totalItems,
		int totalPages) {
}
