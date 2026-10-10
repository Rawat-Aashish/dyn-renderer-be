package com.dynamicformbuilder.submission;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionRepository extends JpaRepository<FormSubmission, Long> {

	Optional<FormSubmission> findByToken(UUID token);

	Optional<FormSubmission> findByIdAndStatus(Long id, SubmissionStatus status);

	Page<FormSubmission> findByFormIdAndStatus(Long formId, SubmissionStatus status, Pageable pageable);

	Page<FormSubmission> findByFormIdAndFormVersionIdAndStatus(Long formId, Long formVersionId,
			SubmissionStatus status, Pageable pageable);

	long countByFormId(Long formId);

	long countByFormIdAndStatus(Long formId, SubmissionStatus status);
}
