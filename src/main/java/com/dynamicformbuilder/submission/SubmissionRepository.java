package com.dynamicformbuilder.submission;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	long countByFormIdAndRatingIsNotNull(Long formId);

	@Query("select avg(s.rating) from FormSubmission s where s.formId = :formId")
	Double averageRating(@Param("formId") Long formId);
}
