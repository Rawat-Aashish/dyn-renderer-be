package com.dynamicformbuilder.condition;

public enum ConditionAction {
	SHOW(Property.VISIBILITY),
	HIDE(Property.VISIBILITY),
	ENABLE(Property.ENABLED),
	DISABLE(Property.ENABLED),
	MAKE_REQUIRED(Property.REQUIRED),
	MAKE_OPTIONAL(Property.REQUIRED);

	/** The field property an action controls. A field may have only one condition per property. */
	public enum Property {
		VISIBILITY, ENABLED, REQUIRED
	}

	private final Property property;

	ConditionAction(Property property) {
		this.property = property;
	}

	public Property property() {
		return property;
	}
}
