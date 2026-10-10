package com.dynamicformbuilder.component;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record ReorderComponentsRequest(
		Long parentSectionId,

		@NotEmpty(message = "Component ids are required")
		List<Long> componentIds) {
}
