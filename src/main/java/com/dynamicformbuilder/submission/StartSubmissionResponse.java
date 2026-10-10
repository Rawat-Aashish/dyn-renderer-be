package com.dynamicformbuilder.submission;

import java.time.LocalDateTime;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record StartSubmissionResponse(
		UUID token,
		String formCode,
		Integer version,
		LocalDateTime startedAt,
		JsonNode configuration) {
}
