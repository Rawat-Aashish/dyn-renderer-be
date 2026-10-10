package com.dynamicformbuilder.submission;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "form_submissions")
@Getter
@Setter
@NoArgsConstructor
public class FormSubmission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private Long formId;

	@Column(nullable = false, updatable = false)
	private Long formVersionId;

	@Column(nullable = false, updatable = false, unique = true)
	private UUID token;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SubmissionStatus status;

	@JdbcTypeCode(SqlTypes.JSON)
	private JsonNode submittedData;

	@Column(nullable = false, updatable = false)
	private LocalDateTime startedAt;

	private LocalDateTime submittedAt;
}
