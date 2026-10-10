package com.dynamicformbuilder.component;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComponentRepository extends JpaRepository<FormComponent, Long> {

	List<FormComponent> findByFormIdOrderByDisplayOrderAscIdAsc(Long formId);

	List<FormComponent> findByFormIdAndParentSectionIdOrderByDisplayOrderAscIdAsc(Long formId, Long parentSectionId);

	Optional<FormComponent> findByIdAndFormId(Long id, Long formId);

	Optional<FormComponent> findByFormIdAndFieldCode(Long formId, String fieldCode);

	boolean existsByFormIdAndFieldCode(Long formId, String fieldCode);

	boolean existsByParentSectionId(Long parentSectionId);
}
