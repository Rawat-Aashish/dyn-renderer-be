package com.dynamicformbuilder.component;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
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
@Table(name = "form_components")
@Getter
@Setter
@NoArgsConstructor
public class FormComponent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private Long formId;

	@Column(nullable = false, length = 100)
	private String fieldCode;

	@Column(nullable = false)
	private String fieldLabel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private ComponentType componentType;

	private String placeholder;

	@Column(length = 500)
	private String helpText;

	@JdbcTypeCode(SqlTypes.JSON)
	private JsonNode defaultValue;

	@Column(nullable = false)
	private Integer displayOrder;

	@Column(name = "is_required", nullable = false)
	private boolean required;

	@Column(name = "is_read_only", nullable = false)
	private boolean readOnly;

	@Column(name = "is_disabled", nullable = false)
	private boolean disabled;

	@Column(name = "is_visible", nullable = false)
	private boolean visible;

	private Long parentSectionId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private ComponentProperties properties;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;
}
