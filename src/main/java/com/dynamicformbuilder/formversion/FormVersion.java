package com.dynamicformbuilder.formversion;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
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
@Table(name = "form_versions")
@Getter
@Setter
@NoArgsConstructor
public class FormVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private Long formId;

	@Column(nullable = false, updatable = false)
	private Integer versionNumber;

	@JdbcTypeCode(SqlTypes.JSON)
	private JsonNode configuration;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FormVersionStatus status;

	private LocalDateTime publishedAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
