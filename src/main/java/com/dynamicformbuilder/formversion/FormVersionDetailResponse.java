package com.dynamicformbuilder.formversion;

import java.time.LocalDateTime;

import tools.jackson.databind.JsonNode;

public record FormVersionDetailResponse(
		Long id,
		Integer versionNumber,
		FormVersionStatus status,
		LocalDateTime publishedAt,
		LocalDateTime createdAt,
		JsonNode configuration) {

	public static FormVersionDetailResponse from(FormVersion version) {
		return new FormVersionDetailResponse(version.getId(), version.getVersionNumber(), version.getStatus(),
				version.getPublishedAt(), version.getCreatedAt(), version.getConfiguration());
	}
}
