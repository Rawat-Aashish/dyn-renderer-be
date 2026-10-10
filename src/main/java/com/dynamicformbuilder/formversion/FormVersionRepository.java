package com.dynamicformbuilder.formversion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormVersionRepository extends JpaRepository<FormVersion, Long> {
}
