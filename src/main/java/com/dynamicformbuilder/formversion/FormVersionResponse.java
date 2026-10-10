package com.dynamicformbuilder.formversion;

import java.time.LocalDateTime;

public record FormVersionResponse(
		Long id,
		Integer versionNumber,
		FormVersionStatus status,
		LocalDateTime publishedAt,
		LocalDateTime createdAt) {

	public static FormVersionResponse from(FormVersion version) {
		return new FormVersionResponse(version.getId(), version.getVersionNumber(), version.getStatus(),
				version.getPublishedAt(), version.getCreatedAt());
	}
}
