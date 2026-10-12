package com.dynamicformbuilder.component;

public enum ComponentType {
	TEXT, TEXTAREA, NUMBER, DECIMAL, EMAIL, PHONE,
	DROPDOWN, RADIO, CHECKBOX, MULTISELECT, TOGGLE,
	DATE, DATETIME,
	FILE_UPLOAD,
	SECTION, HEADING;

	public boolean isLayout() {
		return this == SECTION || this == HEADING;
	}

	public boolean hasOptions() {
		return this == DROPDOWN || this == RADIO || this == CHECKBOX || this == MULTISELECT;
	}

	public boolean isMultiValue() {
		return this == CHECKBOX || this == MULTISELECT;
	}
}
