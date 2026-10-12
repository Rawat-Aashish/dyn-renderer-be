package com.dynamicformbuilder.submission;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "form_submission_files")
@Getter
@Setter
@NoArgsConstructor
public class SubmissionFile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private Long submissionId;

	@Column(nullable = false, updatable = false, length = 100)
	private String fieldCode;

	@Column(nullable = false, updatable = false)
	private String originalName;

	// Relative to app.upload.dir, so the upload folder can be moved without touching the rows
	@Column(nullable = false, updatable = false, length = 500)
	private String storedPath;

	@Column(nullable = false, updatable = false, length = 100)
	private String contentType;

	@Column(nullable = false, updatable = false)
	private Long sizeBytes;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
