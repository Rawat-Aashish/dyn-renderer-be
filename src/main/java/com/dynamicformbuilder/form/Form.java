package com.dynamicformbuilder.form;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "forms")
@Getter
@Setter
@NoArgsConstructor
public class Form {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(nullable = false, unique = true, updatable = false, length = 50)
	private String code;

	@Column(columnDefinition = "text")
	private String description;

	@Column(length = 100)
	private String category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FormStatus status;

	@Column(nullable = false)
	private Integer currentVersion;

	private Integer publishedVersion;

	private LocalDateTime deletedAt;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;
}
