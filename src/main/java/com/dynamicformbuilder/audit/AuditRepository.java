package com.dynamicformbuilder.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends JpaRepository<FormAudit, Long> {

	Page<FormAudit> findByFormId(Long formId, Pageable pageable);
}
