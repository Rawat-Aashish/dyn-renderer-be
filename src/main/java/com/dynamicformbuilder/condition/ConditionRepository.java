package com.dynamicformbuilder.condition;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConditionRepository extends JpaRepository<FormCondition, Long> {

	List<FormCondition> findByFormIdOrderByIdAsc(Long formId);

	Optional<FormCondition> findByIdAndFormId(Long id, Long formId);

	@Query("""
			select distinct c from FormCondition c join c.rules r
			where c.formId = :formId and (c.targetFieldCode = :fieldCode or r.sourceFieldCode = :fieldCode)
			order by c.id
			""")
	List<FormCondition> findUsingField(@Param("formId") Long formId, @Param("fieldCode") String fieldCode);
}
