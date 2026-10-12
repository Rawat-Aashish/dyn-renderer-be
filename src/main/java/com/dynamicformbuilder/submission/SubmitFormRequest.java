package com.dynamicformbuilder.submission;


import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record SubmitFormRequest(
		@NotNull(message = "Token is required")
		UUID token,

		JsonNode data) {
}
