package com.dynamicformbuilder.submission;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionFileRepository extends JpaRepository<SubmissionFile, Long> {

	List<SubmissionFile> findBySubmissionId(Long submissionId);

	Optional<SubmissionFile> findBySubmissionIdAndFieldCode(Long submissionId, String fieldCode);

	Optional<SubmissionFile> findByIdAndSubmissionId(Long id, Long submissionId);
}
