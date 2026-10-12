package com.dynamicformbuilder.form;

import java.time.LocalDateTime;
import java.util.List;

import com.dynamicformbuilder.component.ComponentResponse;
import com.dynamicformbuilder.condition.ConditionResponse;

public record FormDetailResponse(
		Long id,
		String name,
		String code,
		String description,
		String category,
		FormStatus status,
		Integer currentVersion,
		Integer publishedVersion,
		boolean ratingEnabled,
		boolean archived,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<ComponentResponse> components,
		List<ConditionResponse> conditions) {

	public static FormDetailResponse from(Form form, List<ComponentResponse> components,
			List<ConditionResponse> conditions) {
		return new FormDetailResponse(form.getId(), form.getName(), form.getCode(), form.getDescription(),
				form.getCategory(), form.getStatus(), form.getCurrentVersion(), form.getPublishedVersion(),
				form.isRatingEnabled(), form.getDeletedAt() != null, form.getCreatedAt(), form.getUpdatedAt(), components, conditions);
	}
}
