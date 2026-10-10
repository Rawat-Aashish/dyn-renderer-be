package com.dynamicformbuilder.condition;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "form_conditions")
@Getter
@Setter
@NoArgsConstructor
public class FormCondition {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private Long formId;

	@Column(nullable = false, length = 100)
	private String targetFieldCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ConditionAction action;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LogicalOperator logicalOperator;

	// Rules have no life of their own: saving or deleting a condition saves or deletes its rules (Laravel: hasMany + cascade)
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "condition_id", nullable = false)
	@OrderBy("id")
	private List<ConditionRule> rules = new ArrayList<>();

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
