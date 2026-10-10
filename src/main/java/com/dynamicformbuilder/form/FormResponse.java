package com.dynamicformbuilder.form;

import java.time.LocalDateTime;

public record FormResponse(
		Long id,
		String name,
		String code,
		String description,
		String category,
		FormStatus status,
		Integer currentVersion,
		Integer publishedVersion,
		boolean archived,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static FormResponse from(Form form) {
		return new FormResponse(form.getId(), form.getName(), form.getCode(), form.getDescription(),
				form.getCategory(), form.getStatus(), form.getCurrentVersion(), form.getPublishedVersion(),
				form.getDeletedAt() != null, form.getCreatedAt(), form.getUpdatedAt());
	}
}
