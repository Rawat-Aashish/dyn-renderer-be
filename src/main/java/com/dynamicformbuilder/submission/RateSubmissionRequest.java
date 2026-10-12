package com.dynamicformbuilder.submission;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record RateSubmissionRequest(
		@NotNull(message = "Token is required")
		UUID token,

		@NotNull(message = "Rating is required")
		BigDecimal rating) {
}
