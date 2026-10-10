package com.dynamicformbuilder.form;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FormRepository extends JpaRepository<Form, Long> {

	Optional<Form> findByCode(String code);

	boolean existsByName(String name);

	boolean existsByCode(String code);

	boolean existsByNameAndIdNot(String name, Long id);

	@Query("""
			select f from Form f
			where ((:archived = true and f.deletedAt is not null) or (:archived = false and f.deletedAt is null))
			  and (:status is null or f.status = :status)
			  and (lower(f.name) like :search or lower(f.code) like :search)
			""")
	Page<Form> search(@Param("archived") boolean archived, @Param("status") FormStatus status,
			@Param("search") String search, Pageable pageable);
}
