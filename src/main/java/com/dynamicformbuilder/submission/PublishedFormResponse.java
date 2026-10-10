package com.dynamicformbuilder.submission;

import tools.jackson.databind.JsonNode;

public record PublishedFormResponse(String formCode, Integer version, JsonNode configuration) {
}
