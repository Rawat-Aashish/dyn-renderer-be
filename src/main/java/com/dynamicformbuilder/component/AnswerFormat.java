package com.dynamicformbuilder.component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

import tools.jackson.databind.JsonNode;

/**
 * The shape an answer must have for each component type (SPEC.md, "Value format").
 * Used for a component's default value now and for submitted answers later, so both follow one definition.
 */
public final class AnswerFormat {

	public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
			.withResolverStyle(ResolverStyle.STRICT);
	public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm")
			.withResolverStyle(ResolverStyle.STRICT);

	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
	private static final Pattern PHONE_IGNORED = Pattern.compile("[\\s\\-()]");
	private static final Pattern PHONE = Pattern.compile("^\\+?\\d{7,15}$");

	private AnswerFormat() {
	}

	public static boolean isEmpty(JsonNode value) {
		return value == null || value.isNull() || value.isMissingNode()
				|| (value.isString() && value.asString().isEmpty())
				|| (value.isArray() && value.isEmpty());
	}

	/** Returns null when the value has the right shape, otherwise a message saying what is expected. */
	public static String check(ComponentType type, ComponentProperties properties, JsonNode value) {
		return switch (type) {
			case TEXT, TEXTAREA, DROPDOWN, RADIO -> value.isString() ? null : "Must be text";
			case EMAIL -> value.isString() && EMAIL.matcher(value.asString()).matches()
					? null : "Must be a valid email address";
			case PHONE -> value.isString()
					&& PHONE.matcher(PHONE_IGNORED.matcher(value.asString()).replaceAll("")).matches()
							? null : "Must be a valid phone number";
			case NUMBER -> value.isIntegralNumber() ? null : "Must be a whole number";
			case DECIMAL -> checkDecimal(properties, value);
			case CHECKBOX, MULTISELECT -> isArrayOfText(value) ? null : "Must be a list of option values";
			case TOGGLE -> value.isBoolean() ? null : "Must be true or false";
			case DATE -> parses(value, DATE, false) ? null : "Must be a date in the format yyyy-MM-dd";
			case DATETIME -> parses(value, DATE_TIME, true)
					? null : "Must be a date and time in the format yyyy-MM-ddTHH:mm";
			case FILE_UPLOAD -> value.isIntegralNumber() ? null : "Must be the id of an uploaded file";
			case SECTION, HEADING -> "This component does not hold a value";
		};
	}

	private static String checkDecimal(ComponentProperties properties, JsonNode value) {
		if (!value.isNumber()) {
			return "Must be a number";
		}
		int allowed = properties.decimalPlaces();
		if (value.decimalValue().stripTrailingZeros().scale() > allowed) {
			return "Must have at most " + allowed + " decimal places";
		}
		return null;
	}

	private static boolean isArrayOfText(JsonNode value) {
		if (!value.isArray()) {
			return false;
		}
		for (JsonNode item : value) {
			if (!item.isString()) {
				return false;
			}
		}
		return true;
	}

	private static boolean parses(JsonNode value, DateTimeFormatter formatter, boolean withTime) {
		if (!value.isString()) {
			return false;
		}
		try {
			if (withTime) {
				LocalDateTime.parse(value.asString(), formatter);
			} else {
				LocalDate.parse(value.asString(), formatter);
			}
			return true;
		} catch (DateTimeParseException ex) {
			return false;
		}
	}
}
