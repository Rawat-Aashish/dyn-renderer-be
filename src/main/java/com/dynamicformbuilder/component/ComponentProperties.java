package com.dynamicformbuilder.component;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComponentProperties(
		String dataSource,
		List<Option> options,
		Integer decimalPlaces,
		Boolean collapsible) {

	public record Option(String label, String value) {
	}
}
