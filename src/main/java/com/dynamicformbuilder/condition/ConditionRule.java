package com.dynamicformbuilder.condition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "form_condition_rules")
@Getter
@Setter
@NoArgsConstructor
public class ConditionRule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String sourceFieldCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ConditionOperator operator;

	@Column(length = 500)
	private String value;
}
