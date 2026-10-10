package com.dynamicformbuilder.validation;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "form_validation_rules")
@Getter
@Setter
@NoArgsConstructor
public class ValidationRule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private Long componentId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private ValidationRuleType ruleType;

	@Column(nullable = false, length = 500)
	private String ruleValue;

	@Column(length = 500)
	private String errorMessage;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
