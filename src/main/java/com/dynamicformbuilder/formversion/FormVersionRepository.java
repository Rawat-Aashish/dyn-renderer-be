package com.dynamicformbuilder.formversion;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormVersionRepository extends JpaRepository<FormVersion, Long> {

	Page<FormVersion> findByFormId(Long formId, Pageable pageable);

	Optional<FormVersion> findByFormIdAndVersionNumber(Long formId, Integer versionNumber);

	Optional<FormVersion> findByFormIdAndStatus(Long formId, FormVersionStatus status);
}
