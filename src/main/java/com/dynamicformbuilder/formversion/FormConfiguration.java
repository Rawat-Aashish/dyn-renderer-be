package com.dynamicformbuilder.formversion;

import java.util.List;

import com.dynamicformbuilder.component.ComponentResponse;
import com.dynamicformbuilder.condition.ConditionResponse;

/**
 * The snapshot stored in form_versions.configuration when a version is published. It is everything needed
 * to render and validate the form without touching the working tables, which keep changing afterwards.
 */
public record FormConfiguration(
		String name,
		String code,
		String description,
		String category,
		Integer version,
		// Missing in snapshots published before ratings existed; read that as false
		Boolean ratingEnabled,
		List<ComponentResponse> components,
		List<ConditionResponse> conditions) {
}
